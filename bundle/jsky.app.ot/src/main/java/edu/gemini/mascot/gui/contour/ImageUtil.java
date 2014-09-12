/**
 * ImageRenderer.java
 * ----------------------------------------------------------------------------------
 * 
 * Copyright (C) 2008 www.integratedmodelling.org
 * Created: Jun 4, 2008
 *
 * ----------------------------------------------------------------------------------
 * This file is part of ImageMap.
 * 
 * ImageMap is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ImageMap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * ----------------------------------------------------------------------------------
 * 
 * @copyright 2008 www.integratedmodelling.org
 * @author    Sergey Krivov
 * @date      Jun 4, 2008
 * @license   http://www.gnu.org/licenses/gpl.txt GNU General Public License v3
 * @link      http://www.integratedmodelling.org
 **/

//package org.integratedmodelling.utils.image;
package edu.gemini.mascot.gui.contour;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.ImageIO;

/**
 * @author Sergey Krivov
 * 
 */
public class ImageUtil {

	/**
	 * Make a clone of a buffered image
	 * 
	 * @param image
	 * @return
	 */
	public static BufferedImage clone(BufferedImage image) {
		
		String[] pnames = image.getPropertyNames();
		Hashtable<String, Object> cproperties = new Hashtable<String, Object>();
		if (pnames != null) {
			for (int i = 0; i < pnames.length; i++) {
				cproperties.put(pnames[i], image.getProperty(pnames[i]));
			}
		}
		WritableRaster wr = image.getRaster();
		WritableRaster cwr = wr.createCompatibleWritableRaster();
		cwr.setRect(wr);
		BufferedImage cimage = new BufferedImage(image.getColorModel(), // should
																		// be
																		// immutable
				cwr, image.isAlphaPremultiplied(), cproperties);
		
		return cimage;
	}
	
	
	// imgw and imgh are parameters of generated image
	// pixels and rowWidth are parameters of the input data
	public static void createImageFile(int[] pixels, int rowWidth, int imgw,
			int imgh, ColorMap cmap, String fileName ) {
		BufferedImage img = createImage(pixels, rowWidth, imgw, imgh, cmap );
		saveImage(img, fileName);
	}
	
	// start from an existing image; 
	// imgw and imgh are parameters of generated image
	// pixels and rowWidth are parameters of the input data
	public static void createImageFile(BufferedImage image, int[] pixels, int rowWidth, int imgw,
			int imgh, ColorMap cmap, String fileName ) {
		BufferedImage img = createImage(image, pixels, rowWidth, imgw, imgh, cmap );
		saveImage(img, fileName);
	}
	public static int[] upsideDown(int[] pixels, int rowWidth){
		int[] pixelsud= new int[pixels.length];
		int cols = pixels.length/rowWidth;
		int k=0;
		for (int i = cols-1; i>=0 ; i--) {
			for (int j = 0; j < rowWidth; j++){
				pixelsud[k]=pixels[rowWidth*i+j];
				k++;
			}
		}   
		
		return pixelsud;
	}

	public static void createImageFile(int[][] pixels, int imgw, int imgh,
			ColorMap cmap, String fileName, boolean upsideDown) {
		BufferedImage img = createImage(pixels, imgw, imgh, cmap, upsideDown);
		saveImage(img, fileName);
	}

	public static BufferedImage createImage(int[] pixels, int rowWidth,
			int imgw, int imgh, ColorMap cmap) {
		
		MemoryImageSource source = new MemoryImageSource(rowWidth,
				pixels.length / rowWidth, cmap.getColorModel(), pixels, 0,
				rowWidth);
		Toolkit tk = Toolkit.getDefaultToolkit();
		return toBufferedImage(tk.createImage(source), imgw, imgh);
	}
	
	public static Image drawUnscaledRaster(int[] pixels, int rowWidth, ColorMap cmap) {
		
		MemoryImageSource source = new MemoryImageSource(rowWidth,
				pixels.length / rowWidth, cmap.getColorModel(), pixels, 0,
				rowWidth);
		Toolkit tk = Toolkit.getDefaultToolkit();
		return tk.createImage(source);
	}
	

	public static BufferedImage createImage(BufferedImage image, 
			int[] pixels, int rowWidth,
			int imgw, int imgh, ColorMap cmap) {
		// TODO
		MemoryImageSource source = new MemoryImageSource(rowWidth,
				pixels.length / rowWidth, cmap.getColorModel(), pixels, 0,
				rowWidth);
		Toolkit tk = Toolkit.getDefaultToolkit();
		return toBufferedImage(tk.createImage(source), imgw, imgh);
	}
	
	
	public static BufferedImage createImage(int[][] pixels, int imgw,
			int imgh, ColorMap cmap, boolean upsideDown) {
		int rowWidth = pixels[0].length;
		int[] pixels1d = new int[pixels.length * rowWidth];
		int k = 0;
		if (!upsideDown) {
			for (int i = 0; i < pixels.length; i++) {
				for (int j = 0; j < pixels[i].length; j++) {
					pixels1d[k] = pixels[i][j];
					k++;
					// pixels1d[k] should be faster than pixels1d[rowWidth*i+j
					// ]???
				}
			}
		} else {
			for (int i = pixels.length ; i >=0; i--) {
				for (int j = 0; j < pixels[i].length; j++) {
					pixels1d[k] = pixels[i][j];
					k++;
					// pixels1d[k] should be faster than pixels1d[rowWidth*i+j
					// ]???
				}
			}

		}
		return createImage(pixels1d, rowWidth, imgw, imgh, cmap);
	}

	public static void saveImage(BufferedImage bimg, String fileName) {

		try {
			File outputfile = new File(fileName);
			String ext = fileName.substring(fileName.lastIndexOf('.') + 1,
					fileName.length());

			ImageIO.write(bimg, ext, outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// The rest of the class is pretty much is inside the besball

	// This method returns a buffered image with the contents of an image
	private static BufferedImage toBufferedImage(Image image, int w, int h) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}

		// Determine if the image has transparent pixels; for this method's
		// implementation, see e661 Determining If an Image Has Transparent
		// Pixels
		boolean hasAlpha = hasAlpha(image);

		// Create a buffered image with a format that's compatible with the
		// screen
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		try {
			// Determine the type of transparency of the new buffered image
			int transparency = Transparency.OPAQUE;
			if (hasAlpha) {
				transparency = Transparency.BITMASK;
			}

			// Create the buffered image
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(w, h, transparency);
		} catch (HeadlessException e) {
			// The system does not have a screen
		}

		if (bimage == null) {
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
			bimage = new BufferedImage(w, h, type); // image.getWidth(null),
													// image.getHeight(null),
													// type);
		}

		// Copy image to buffered image
		Graphics g = bimage.createGraphics();

		// Paint the image onto the buffered image
		g.drawImage(image, 0, 0, w, h, null); // /??? scaling
		g.dispose();

		return bimage;
	}

	// This method returns true if the specified image has transparent pixels
	private static boolean hasAlpha(Image image) {
		// If buffered image, the color model is readily available
		if (image instanceof BufferedImage) {
			BufferedImage bimage = (BufferedImage) image;
			return bimage.getColorModel().hasAlpha();
		}

		// Use a pixel grabber to retrieve the image's color model;
		// grabbing a single pixel is usually sufficient
		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try {
			pg.grabPixels();
		} catch (InterruptedException e) {
		}

		// Get the image's color model
		ColorModel cm = pg.getColorModel();
		return cm.hasAlpha();
	}

	// examples

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// generate data: Russian (Dutch ) flag

		int w = 150;
		int h = 99;
		int size = w * h;
		int[] pixs = new int[size];
		for (int i = 0; i < pixs.length; i++) {
			if (i < size / 3) {
				pixs[i] = 0;
			} else if (i < 2 * size / 3) {
				pixs[i] = 1;
			} else {
				pixs[i] = 2;
			}
		}

		// you need to create a color map

		// the first argument is newer greater than 16 and less than 1
		ColorMap cmap = new ColorMap(16, new Color[] { Color.WHITE, Color.BLUE,
				Color.RED });

		ColorMap cmapDutch = new ColorMap(16, new Color[] { Color.BLUE,
				Color.WHITE, Color.RED });

		ImageUtil.createImageFile(pixs, w, w, h, cmap, "russianFlag.png");
		ImageUtil.createImageFile(pixs, w, w, h, cmapDutch, "dutchFlag.png");
		
		ImageUtil.createImageFile(upsideDown(pixs, w), w, w, h, cmap, "russianFlagUpsideDown.png");

		// rescaled version big
		ImageUtil.createImageFile(pixs, w, 2 * w, 2 * h, cmap,
				"russianFlagBig.png");
		// rescaled version small
		ImageUtil.createImageFile(pixs, w, w / 2, h / 2, cmap,
				"russianFlagSmal.png");
		
		//reverse
		ImageUtil.createImageFile(upsideDown(pixs, w), w, 2 * w, 2 * h, cmap,
		"russianFlagBigUpsideDown.png");

	}

}
