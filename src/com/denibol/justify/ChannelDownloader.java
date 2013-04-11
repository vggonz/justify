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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.protocol.Protocol;
import de.felixbruns.jotify.protocol.channel.Channel;
import de.felixbruns.jotify.protocol.channel.ChannelListener;

public class ChannelDownloader implements ChannelListener{
	/* 
	 * Cipher implementation, key and IV
	 * for decryption of audio stream.
	 */
	private Cipher _cipher;
	private Key    _key;
	private byte[] _iv;
	
	/* Streams, audio decoding and output. */
	private PipedInputStream  _input;
	private PipedOutputStream _output;
	
	/* 
	 * Protocol, Track object and variables for
	 * substream requesting and handling.
	 */
	private Protocol _protocol;
	private Track    _track;
	private int      _streamOffset;
	private int      _streamLength;
	private int      _receivedLength;
	private boolean  _loading;
	
	/* Caching of substreams. */
	private byte[]         _cacheData;
	
	/**
	 * Creates a new ChannelPlayer for decrypting and playing audio from a
	 * protocol channel.
	 * 
	 * @param protocol A {@link Protocol} instance which will be used to
	 *                 communicate with the server.
	 * @param track    A {@link Track} object identifying the track to be
	 *                 streamed.
	 * @param key      The corresponding AES key for decrypting the stream.
	 * 
	 * @see Protocol
	 * @see Track
	 */
	public ChannelDownloader(Protocol protocol, Track track, byte[] key, FileOutputStream file){
		/* Set protocol, track, playback listener and cache. */
		_protocol = protocol;
		_track    = track;
		
		/* Initialize AES cipher. */		
		try{
			/* Get AES cipher instance. */
			_cipher = Cipher.getInstance("AES/CTR/NoPadding");
			
			/* Create secret key from bytes and set initial IV. */
			_key = new SecretKeySpec(key, "AES");
			_iv  = new byte[]{
				(byte)0x72, (byte)0xe0, (byte)0x67, (byte)0xfb,
				(byte)0xdd, (byte)0xcb, (byte)0xcf, (byte)0x77,
				(byte)0xeb, (byte)0xe8, (byte)0xbc, (byte)0x64,
				(byte)0x3f, (byte)0x63, (byte)0x0d, (byte)0x93
			};
			
			/* Initialize cipher with key and IV in encrypt mode. */
			_cipher.init(Cipher.ENCRYPT_MODE, _key, new IvParameterSpec(_iv));
			
		}catch(NoSuchAlgorithmException e){ throw new RuntimeException("AES/CTR is not available!", e);
		}catch(NoSuchPaddingException e){ throw new RuntimeException("'NoPadding' is not available! mmh. yeah.", e);
		}catch (InvalidKeyException e){ throw new RuntimeException("Invalid key!", e);
		}catch (InvalidAlgorithmParameterException e){ throw new RuntimeException("Invalid IV!", e); }
		
		/* Create piped streams and connect them (10 seconds, 160 kbit ogg buffer). */
		try{
			_input  = new PipedInputStream(160 * 1024 * 10 / 8);
			_output = new PipedOutputStream(_input);
		}
		catch(IOException e){ throw new RuntimeException("Can't connect piped streams!", e); }
		
		/* Set substream offset and length (5 seconds, 160 kbit ogg data). */
		_streamOffset = 0;
		_streamLength = 160 * 1024 * 5 / 8;
		_loading      = false;
		
		/* 
		 * Send first substream request so we can provide
		 * enough data on the piped output stream. 
		 */
		try{
			
			_loading = true;
			_protocol.sendSubstreamRequest(this, _track, _streamOffset, _streamLength);
			
		}catch(ProtocolException e){ throw new RuntimeException("Error sending substream request!", e); }
		
		/* Open input stream for playing. */
		if(!this.open(_input, file)) throw new RuntimeException("Can't open input stream for playing!");
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
			
			byte[] buffer = new byte[1024];
			int read = 0;
			
			while(read != -1){
				
				/* Do the actual check, but only if we're not loading data right now. */
				if(!_loading){
					/* Set flag that we're loading data now. */
					_loading = true;
					
					/* Increment substream offset. */
					_streamOffset += _streamLength;
					
					try{ _protocol.sendSubstreamRequest(this, _track, _streamOffset, _streamLength);
					}catch(ProtocolException e){
						System.err.println("Error sending substream request!");	
						return false;
					}

				}

				// Read data from audio stream and write it to the audio line.
				try{
					if((read = stream.read(buffer, 0, buffer.length)) > 0){
						file.write(buffer);
					}
				}catch(IOException e){ e.printStackTrace(); }
			}
			
		}catch(IOException e){ return false; }
		
		/* Success. */
		return true;
	}
	
	/* Called when a channel header is received. */
	public void channelHeader(Channel channel, byte[] header){
		/* Create buffer for this substream. */
		_cacheData = new byte[_streamLength];
		
		/* We didn't receive data yet. */
		_receivedLength = 0;
	}
	
	/* Called when channel data is received. */
	public void channelData(Channel channel, byte[] data){
		/* Offsets needed for deinterleaving. */
		int off, w, x, y, z;
		
		/* Copy data to cache buffer. */
		for(int i = 0; i < data.length; i++) _cacheData[_receivedLength + i] = data[i];
		
		/* Allocate space for ciphertext. */
		byte[] ciphertext = new byte[data.length + 1024];
		byte[] keystream  = new byte[16];
		
		/* Decrypt each 1024 byte block. */
		for(int block = 0; block < data.length / 1024; block++){
			/* Deinterleave the 4x256 byte blocks. */
			off = block * 1024;
			w	= block * 1024 + 0 * 256;
			x	= block * 1024 + 1 * 256;
			y	= block * 1024 + 2 * 256;
			z	= block * 1024 + 3 * 256;
			
			for(int i = 0; i < 1024 && (block * 1024 + i) < data.length; i += 4){
				ciphertext[off++] = data[w++];
				ciphertext[off++] = data[x++];
				ciphertext[off++] = data[y++];
				ciphertext[off++] = data[z++];
			}
			
			/* Decrypt 1024 bytes block. This will fail for the last block. */
			for(int i = 0; i < 1024 && (block * 1024 + i) < data.length; i += 16){
				/* Produce 16 bytes of keystream from the IV. */
				try{ 
					keystream = _cipher.doFinal(_iv);
				
				}catch(IllegalBlockSizeException e){ e.printStackTrace();
				}catch(BadPaddingException e){ e.printStackTrace(); }
				
				/* 
				 * Produce plaintext by XORing ciphertext with keystream.
				 * And somehow I also need to XOR with the IV... Please
				 * somebody tell me what I'm doing wrong, or is it the
				 * Java implementation of AES? At least it works like this.
				 */
				for(int j = 0; j < 16; j++){
					ciphertext[block * 1024 + i + j] ^= keystream[j] ^ _iv[j];
				}

				/* Update IV counter. */
				for(int j = 15; j >= 0; j--){
					_iv[j] += 1;
					
					if((int)(_iv[j] & 0xFF) != 0) break;
				}
				
				/* Set new IV. */
				try{
					_cipher.init(Cipher.ENCRYPT_MODE, _key, new IvParameterSpec(_iv));
					
				}catch(InvalidKeyException e){ e.printStackTrace();
				}catch(InvalidAlgorithmParameterException e){ e.printStackTrace(); }
			}
		}
		
		/* Write data to output stream. */
		try{ _output.write(ciphertext, 0, ciphertext.length - 1024);
		}catch(IOException e){ /* Just don't care... */ }
		
		_receivedLength += data.length;
	}
	
	/* Called when a channel end is reached. */
	public void channelEnd(Channel channel){
		
		Channel.unregister(channel.getId());
		
		/* Loading complete. */
		_loading = false;
		
		if(_receivedLength < _streamLength){
			try{ _output.close();
			}catch(IOException e){ e.printStackTrace(); }
		}
	}
	
	/* Called when a channel error occurs. */
	public void channelError(Channel channel){ /* Just ignore channel errors. */ }
}
