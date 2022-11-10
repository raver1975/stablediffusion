// Copyright (c) 2016-present boyw165 & tlin4194
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.klemstinegroup;

import android.graphics.Bitmap;
import android.graphics.Color;

public class CvUtil {

    public static void toColors(Bitmap from,
                                int[][] to) {
        int width = from.getWidth();
        int height = from.getHeight();

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                to[x][y] = from.getPixel(x, y);
            }
        }
    }

    public static Bitmap toBitmap(int[][] from) {
        int width = from.length;
        int height = from[0].length;
        Bitmap bmp = Bitmap.createBitmap(width,
                                         height,
                                         Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                bmp.setPixel(x, y, from[x][y]);
            }
        }

        return bmp;
    }

    public static void calcEnergyMap(int[][] from,
                                     int[][] to,
                                     int width,
                                     int height) {
        // Use the Sobel kernel.
        final int[][] Gx = new int[][]{
            {-1, 0, +1},
            {-2, 0, +2},
            {-1, 0, +1}};
        final int[][] Gy = new int[][]{
            {+1, +2, +1},
            {+0, +0, +0},
            {-1, -2, -1}};

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                to[x][y] = conv2D(from, x, y, width, height, Gx, Gy);
            }
        }
    }

    // FIXME:
    public static int[] findVerticalSeam(int[][] energyMap,
                                         int width) {
        int height = energyMap[0].length;
        // Find out the minimum index x on the first row.
        int minX = energyMap[0][0];
        for (int x = 1; x < width; ++x) {
            minX = Math.min(minX, energyMap[x][0]);
        }

        // Construct the lookup table from top to bottom.
        int[][] lookupTable = new int[width][height];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                if (y == 0) {
                    lookupTable[x][y] = energyMap[x][y];
                } else {
                    int left = Math.max(0, x - 1);
                    int middle = x;
                    int right = Math.min(width - 1, x + 1);
                    int preY = y - 1;
                    int minVal = Math.min(lookupTable[left][preY],
                                        Math.min(lookupTable[middle][preY],
                                                 lookupTable[right][preY]));

                    lookupTable[x][y] = minVal + energyMap[x][y];
                }
            }
        }

        // Find out the minimum index x on the last row.
        minX = energyMap[0][height - 1];
        for (int x = 1; x < width; ++x) {
            minX = Math.min(minX, energyMap[x][height - 1]);
        }

        // Find the seam from bottom to top.
        int[] seam = new int[height];
        seam[height - 1] = minX;
        for (int y = height - 2; y > 0; --y) {
            int preX = seam[y + 1];
            int[] neighbors = new int[]{
                // left
                Math.max(0, preX - 1),
                // middle
                preX,
                // right
                Math.min(width - 1, preX + 1)};
            int x = neighbors[0];
            int cost = lookupTable[x][y];
            seam[y] = x;
            for (int i = 1; i < neighbors.length; ++i) {
                x = neighbors[i];
                if (lookupTable[x][y] < cost) {
                    seam[y] = x;
                    cost = lookupTable[x][y];
                }
            }
        }

        return seam;
    }

    public static void addSeam(int[][] from,
                               int[][] to,
                               int[] seam) {
        int width = from.length;
        int height = from[0].length;

        // 1 | 2     1 | 1 | 2
        // --+--  => --+---+--
        // 3 | 4     3 | 4 | 4

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (x == seam[y]) {
                    to[x][y] = from[x][y];
                    to[Math.min(x + 1, width - 1)][y] = from[x][y];

                } else if (x > seam[y]) {
                    to[Math.min(x + 1, width - 1)][y] = from[x][y];
                } else {
                    to[x][y] = from[x][y];
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private static int conv2D(int[][] from,
                              int x,
                              int y,
                              int width,
                              int height,
                              int[][] Gx,
                              int[][] Gy) {
        int resultGx = 0;
        int resultGy = 0;
        int grayscale;

        // [-1, -1]
        grayscale = grayscale(from, x - 1, y - 1, width, height);
        resultGx += Gx[0][0] * grayscale;
        resultGy += Gy[0][0] * grayscale;
        // [+0, -1]
        grayscale = grayscale(from, x, y - 1, width, height);
        resultGx += Gx[1][0] * grayscale;
        resultGy += Gx[1][0] * grayscale;
        // [+1, -1]
        grayscale = grayscale(from, x + 1, y - 1, width, height);
        resultGx += Gx[2][0] * grayscale;
        resultGy += Gx[2][0] * grayscale;

        // [-1, +0]
        grayscale = grayscale(from, x - 1, y, width, height);
        resultGx += Gx[0][1] * grayscale;
        resultGy += Gx[0][1] * grayscale;
        // [+0, +0]
        grayscale = grayscale(from, x, y, width, height);
        resultGx += Gx[1][1] * grayscale;
        resultGy += Gx[1][1] * grayscale;
        // [+1, +0]
        grayscale = grayscale(from, x + 1, y, width, height);
        resultGx += Gx[2][1] * grayscale;
        resultGy += Gx[2][1] * grayscale;

        // [-1, +1]
        grayscale = grayscale(from, x - 1, y + 1, width, height);
        resultGx += Gx[0][2] * grayscale;
        resultGy += Gx[0][2] * grayscale;
        // [+0, +1]
        grayscale = grayscale(from, x, y + 1, width, height);
        resultGx += Gx[1][2] * grayscale;
        resultGy += Gx[1][2] * grayscale;
        // [+1, +1]
        grayscale = grayscale(from, x + 1, y + 1, width, height);
        resultGx += Gx[2][2] * grayscale;
        resultGy += Gx[2][2] * grayscale;

        return (int) Math.hypot((double) resultGx,
                                (double) resultGy);
    }

    private static int grayscale(int[][] from,
                                 int x,
                                 int y,
                                 int width,
                                 int height) {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0;

        int r = Color.red(from[x][y]);
        int g = Color.green(from[x][y]);
        int b = Color.blue(from[x][y]);

        return (r + g + b) / 3;
    }
}