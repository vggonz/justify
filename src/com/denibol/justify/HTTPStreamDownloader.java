/*
   Copyright (c) 2009, Felix Bruns <felixbruns@web.de>
   All rights reserved.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE AUTHORS ''AS IS'' AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
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

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.http.DecryptingInputStream;
import de.felixbruns.jotify.player.http.DeinterleavingInputStream;

public class HTTPStreamDownloader {
	
	public HTTPStreamDownloader(List<String> urls, Track track, byte[] key, FileOutputStream file){
		
		/* Build a list of input streams. */
		Collection<InputStream> streams = new ArrayList<InputStream>();
		
		for(String url : urls){
			try{
				/* Open connection to HTTP stream URL and set request headers. */
				URLConnection connection = new URL(url).openConnection();
				
				connection.setRequestProperty("User-Agent", "Justify-Java/0.1/99998");
				connection.setRequestProperty(
					"Range", "bytes=0-" + Math.round(160.0 * 1024.0 * track.getLength() / 1000.0 / 8.0 / 4.0)
				);
				
				streams.add(connection.getInputStream());
			}
			catch(IOException e){
				throw new RuntimeException("Can't open connection to HTTP stream!", e);
			}
		}
		
		InputStream audioStream = new BufferedInputStream(
			new DecryptingInputStream(
				new DeinterleavingInputStream(streams),
				key
			)
		);
		
		/* Open input stream for playing. */
		if(!this.open(audioStream, file)){
			throw new RuntimeException("Can't open input stream for playing!");
		}
	}
	
	/* Open an input stream and start decoding it,
	 * set up audio stuff when AudioInputStream
	 * was sucessfully created.
	 */
	private boolean open(InputStream stream, FileOutputStream file){
		
		/* Spotify specific ogg header. */
		byte[] header = new byte[167];
		
		try{
			/* Read and decode header. */
			stream.read(header);
			
			/* Buffer for data and number of bytes read */
			byte[] buffer = new byte[1024];
			int read = 0;
			
			/* Read-write loop. */
			while(read != -1){

				/* Read data from audio stream and write it to the audio line. */
				try{
					if((read = stream.read(buffer, 0, buffer.length)) > 0){
						file.write(buffer);
					}
				}
				catch(IOException e){
					e.printStackTrace();
					
					/* Don't care. */
				}
				
			}
		}
		catch(IOException e){
			e.printStackTrace();
			return false;
		}

		/* Success. */
		return true;
	}
	
}
