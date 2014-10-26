package com.twilight.h264.player;

import static com.twilight.h264.decoder.H264Context.NAL_AUD;
import static com.twilight.h264.decoder.H264Context.NAL_IDR_SLICE;
import static com.twilight.h264.decoder.H264Context.NAL_SLICE;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import javax.swing.JFrame;

import com.twilight.h264.decoder.AVFrame;
import com.twilight.h264.decoder.AVPacket;
import com.twilight.h264.decoder.H264Decoder;
import com.twilight.h264.decoder.MpegEncContext;
import com.twilight.h264.util.FrameUtils;

public class H264Player implements Runnable {
	
	public static final int INBUF_SIZE = 65535;
	private PlayerFrame displayPanel;
	private String fileName;
	private int[] buffer = null;
	boolean foundFrameStart;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new H264Player(args);
	}

	public H264Player(String[] args) {
		if(args.length<1) {
			System.out.println("Usage: java com.twilight.h264.decoder.H264Player <.h264 raw file>\n");
			return;
		}

		JFrame frame = new JFrame("Player");
		displayPanel = new PlayerFrame();

		frame.getContentPane().add(displayPanel, BorderLayout.CENTER);

		// Finish setting up the frame, and show it.
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		displayPanel.setVisible(true);
		frame.pack();
		frame.setVisible(true);
		frame.setSize(new Dimension(645, 380));
		
		fileName = args[0];

		new Thread(this).start();
	}
	
	@Override
	public void run() {
		System.out.println("Playing "+ fileName);
		playFile(fileName);		
	}

	private boolean isEndOfFrame(int code) {
		int nal = code & 0x1F;

		if (nal == NAL_AUD) {
			foundFrameStart = false;
			return true;
		}

		boolean foundFrame = foundFrameStart;
		if (nal == NAL_SLICE || nal == NAL_IDR_SLICE) {
			if (foundFrameStart) {
				return true;
			}
			foundFrameStart = true;
		} else {
			foundFrameStart = false;
		}

		return foundFrame;
	}

	public boolean playFile(String filename) {
	    H264Decoder codec;
	    MpegEncContext c= null;
	    FileInputStream fin = null;
	    int frame, len;
	    int[] got_picture = new int[1];
	    File f = new File(filename);
	    AVFrame picture;
	    //uint8_t inbuf[INBUF_SIZE + H264Context.FF_INPUT_BUFFER_PADDING_SIZE];
	    byte[] inbuf = new byte[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
	    int[] inbuf_int = new int[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
	    AVPacket avpkt = new AVPacket();

	    avpkt.av_init_packet();

	    /* set end of buffer to 0 (this ensures that no overreading happens for damaged mpeg streams) */
	    Arrays.fill(inbuf, INBUF_SIZE, MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE + INBUF_SIZE, (byte)0);

	    System.out.println("Video decoding\n");

	    /* find the mpeg1 video decoder */
	    codec = new H264Decoder();
	    if (codec == null) {
	    	System.out.println("codec not found\n");
	        System.exit(1);
	    } // if

	    c= MpegEncContext.avcodec_alloc_context();
	    picture= AVFrame.avcodec_alloc_frame();

	    if((codec.capabilities & H264Decoder.CODEC_CAP_TRUNCATED)!=0)
	        c.flags |= MpegEncContext.CODEC_FLAG_TRUNCATED; /* we do not send complete frames */

	    /* For some codecs, such as msmpeg4 and mpeg4, width and height
	       MUST be initialized there because this information is not
	       available in the bitstream. */

	    /* open it */
	    if (c.avcodec_open(codec) < 0) {
	    	System.out.println("could not open codec\n");
	        System.exit(1);
	    }

	    try {
		    /* the codec gives us the frame size, in samples */
		    fin = new FileInputStream(f);

		    frame = 0;
		    int dataPointer;
		    int fileOffset = 0;
		    foundFrameStart = false;

		    // avpkt must contain exactly 1 NAL Unit in order for decoder to decode correctly.
	    	// thus we must read until we get next NAL header before sending it to decoder.
			// Find 1st NAL
			int[] cacheRead = new int[5];
			cacheRead[0] = fin.read();
			cacheRead[1] = fin.read();
			cacheRead[2] = fin.read();
			cacheRead[3] = fin.read();

			while(!(
					cacheRead[0] == 0x00 &&
					cacheRead[1] == 0x00 &&
					cacheRead[2] == 0x00 &&
					cacheRead[3] == 0x01
					)) {
				 cacheRead[0] = cacheRead[1];
				 cacheRead[1] = cacheRead[2];
				 cacheRead[2] = cacheRead[3];
				 cacheRead[3] = fin.read();
			} // while

			boolean hasMoreNAL = true;
			cacheRead[4] = fin.read();

			// 4 first bytes always indicate NAL header
			while (hasMoreNAL) {
				inbuf_int[0] = cacheRead[0];
				inbuf_int[1] = cacheRead[1];
				inbuf_int[2] = cacheRead[2];
				inbuf_int[3] = cacheRead[3];
				inbuf_int[4] = cacheRead[4];

				dataPointer = 5;
				// Find next NAL
				cacheRead[0] = fin.read();
				if (cacheRead[0]==-1) hasMoreNAL = false;
				cacheRead[1] = fin.read();
				if (cacheRead[1]==-1) hasMoreNAL = false;
				cacheRead[2] = fin.read();
				if (cacheRead[2]==-1) hasMoreNAL = false;
				cacheRead[3] = fin.read();
				if (cacheRead[3]==-1) hasMoreNAL = false;
				cacheRead[4] = fin.read();
				if (cacheRead[4]==-1) hasMoreNAL = false;
				while(!(
						cacheRead[0] == 0x00 &&
						cacheRead[1] == 0x00 &&
						cacheRead[2] == 0x00 &&
						cacheRead[3] == 0x01 &&
						isEndOfFrame(cacheRead[4]) 
						) && hasMoreNAL) {
					 inbuf_int[dataPointer++] = cacheRead[0];
					 cacheRead[0] = cacheRead[1];
					 cacheRead[1] = cacheRead[2];
					 cacheRead[2] = cacheRead[3];
					 cacheRead[3] = cacheRead[4];
					 cacheRead[4] = fin.read();
					if (cacheRead[4]==-1) hasMoreNAL = false;
				} // while

				avpkt.size = dataPointer;
				//System.out.println(String.format("Offset 0x%X, packet size 0x%X, nal=0x%X", fileOffset, dataPointer, inbuf_int[4] & 0x1F));
				fileOffset += dataPointer - 1;

		        avpkt.data_base = inbuf_int;
		        avpkt.data_offset = 0;

		        try {
			        while (avpkt.size > 0) {
			            len = c.avcodec_decode_video2(picture, got_picture, avpkt);
			            if (len < 0) {
			                System.out.println("Error while decoding frame "+ frame);
			                // Discard current packet and proceed to next packet
			                break;
			            } // if
			            if (got_picture[0]!=0) {
			            	picture = c.priv_data.displayPicture;

			            	int imageWidth = picture.imageWidthWOEdge;
			            	int imageHeight = picture.imageHeightWOEdge;
							int bufferSize = imageWidth * imageHeight;
							if (buffer == null || bufferSize != buffer.length) {
								buffer = new int[bufferSize];
							}
							FrameUtils.YUV2RGB_WOEdge(picture, buffer);			
							displayPanel.lastFrame = displayPanel.createImage(new MemoryImageSource(imageWidth
									, imageHeight, buffer, 0, imageWidth));
							displayPanel.invalidate();
							displayPanel.updateUI();			            	
			            }
			            avpkt.size -= len;
			            avpkt.data_offset += len;
			        }
		        } catch(Exception ie) {
		        	// Any exception, we should try to proceed reading next packet!
		        	ie.printStackTrace();
		        } // try

			} // while


	    } catch(Exception e) {
	    	e.printStackTrace();
	    } finally {
	    	try { fin.close(); } catch(Exception ee) {}
	    } // try

	    c.avcodec_close();
	    c = null;
	    picture = null;
	    System.out.println("Stop playing video.");

	    return true;
	}
}
