/*
    This file is part of Justify.

    Justify is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Justify is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.denibol.justify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.lang.reflect.Field;

import de.felixbruns.jotify.JotifyConnection;
import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.media.Link;
import de.felixbruns.jotify.media.Link.InvalidSpotifyURIException;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.channel.ChannelCallback;

public class Justify {

	public static void main(String args[]){
		if (args.length != 3){
			System.err.println("[ERROR] Se esperan 3 parámetros: nombre de usuario, contraseña y dirección Spotify para descargar");
			return;
		}
		
		JotifyConnection connection = new JotifyConnection();
		try{
			try{
				connection.login(args[0], args[1]);
			}catch(ConnectionException ce){ throw new JustifyException("[ERROR] No se ha podido conectar con el servidor");
			}catch(AuthenticationException ae){ throw new JustifyException("[ERROR] Usuario o contraseña no válidos"); }
			new Thread(connection, "JotifyConnection-Thread").start();

			System.out.println(connection.user());
			if (!connection.user().isPremium()) throw new JustifyException("[ERROR] Debes ser usuario 'premium'");
			try{
				Link uri = Link.create(args[2]);
				if (uri.isTrackLink()){
					
					Track track = connection.browseTrack(uri.getId());
					if (track == null) throw new JustifyException("[ERROR] Pista no encontrada");
					downloadTrack(track, connection, null);
					
				}else if (uri.isPlaylistLink()){
					
					Playlist playlist = connection.playlist(uri.getId());
					if (playlist == null) throw new JustifyException("[ERROR] Lista de reproducción no encontrada");
					System.out.println(playlist);
					System.out.println("Número de pistas: " + playlist.getTracks().size());
					for(Track track : playlist.getTracks()) downloadTrack(connection.browse(track), connection, playlist.getAuthor() + " - " + playlist.getName());
					
				}else if(uri.isAlbumLink()){
					
					Album album = connection.browseAlbum(uri.getId());
					if (album == null) throw new JustifyException("[ERROR] Álbum no encontrado");
					System.out.println(album);
					System.out.println("Contiene " + album.getTracks().size() + " pistas repartidas en " + album.getDiscs().size() + " disco(s)");
					for(Track track : album.getTracks()) downloadTrack(track, connection, album.getArtist().getName() + " - " + album.getName());
					
				}else throw new JustifyException("[ERROR] Se esperaba una pista, álbum o lista de reproducción");
				
			}catch (InvalidSpotifyURIException urie){ throw new JustifyException("[ERROR] Dirección de Spotify no válida"); }
				
		}catch (JustifyException je){ System.err.println(je.getMessage()); je.printStackTrace();
		}catch (TimeoutException te){ System.err.println(te.getMessage()); te.printStackTrace();
		}finally{
			try{ connection.close();
			}catch (ConnectionException ce){ System.err.println("[ERROR] No se ha podido desconectar"); } 
		}
	}

	public static void downloadTrack(Track track, JotifyConnection connection, String parent) throws JustifyException, TimeoutException{
		System.out.println(track);
		selectBitrate(track, HIGH_QUALITY);
		try{
			String nombre = track.getArtist().getName() + " - " + track.getTitle() + ".ogg";
			File file = new File(parent, nombre);
			System.out.println("Descargando al fichero " + file.getPath());
			if (parent != null){
				File dir = new File(parent);
				dir.mkdir();
			}
			file.createNewFile();
			download(connection, track, file);
		}catch(FileNotFoundException fnfe){ fnfe.printStackTrace(); /* throw new JustifyException("[ERROR] No se ha podido guardar el archivo"); */
		}catch(IOException ioe){ ioe.printStackTrace(); /* throw new JustifyException("[ERROR] Ha ocurrido un fallo de entrada / salida"); */ }

	}

	private static final int HIGH_QUALITY = 320000; // 320 kbps
	private static final int MEDIUM_QUALITY = 160000; // 160 kbps
	private static final int LOW_QUALILTY = 96000; // 96 kbps

	// Se ordenan los ficheros asociados a la pista para que esté primero el de la calidad requerida
	// porque Protocol.sendAesKeyRequest solo pide el primero
	private static void selectBitrate(Track track, int bitrate){
		de.felixbruns.jotify.media.File selected = null;
		int diff = Integer.MAX_VALUE;
		for(de.felixbruns.jotify.media.File f : track.getFiles()){
			int new_diff = Math.abs(f.getBitrate() - bitrate);
			if (new_diff < diff){
				diff = new_diff;
				selected = f;
			}
		}

		if (selected != null){
			track.getFiles().remove(selected);
			track.getFiles().add(0, selected);
		}
	}

	public static void download(JotifyConnection connection, Track track, java.io.File file) throws FileNotFoundException, TimeoutException{
		/* Create channel callbacks. */
		ChannelCallback       callback       = new ChannelCallback();
		Protocol protocol = getProtocol(connection);
		int timeout = 10;
		FileOutputStream fos = new FileOutputStream(file);
		
		/* Send play request (token notify + AES key). */
		try{ protocol.sendPlayRequest(callback, track);
		}catch(ProtocolException e){ return; }
		
		/* Get AES key. */
		byte[] key = callback.get(timeout, TimeUnit.SECONDS);
		
		new ChannelDownloader(protocol, track, key, fos);
	}

	// Recupera el atributo Protocol de la conexión necesario para utilizar el ChannelPlayer 
	// personalizado, mediante Reflection
	public static Protocol getProtocol(JotifyConnection connection){
		try{
			Class clase = connection.getClass();
			Field campo = clase.getDeclaredField("protocol");
			campo.setAccessible(true);
			return (Protocol)campo.get(connection);
		}catch(Exception e){ return null; }
	}
	
}
