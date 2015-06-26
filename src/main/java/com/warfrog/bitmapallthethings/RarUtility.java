/*
*
* BitTwiddler - BMP transcoder
* Copyright (C) 2015  Tyler Pitchford
*
* This file is part of BitTwiddler.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; see the file COPYING.  If not, write to
* the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
*
*/

package com.warfrog.bitmapallthethings;

import java.io.File;

public class RarUtility {
    //hack to see if rar is available
    public static boolean isRarAvailable(String location) {
        boolean returnValue;
        if(location != null) {
            returnValue = CommandUtility.isCommandAvailable(location + File.separator + "rar");
        } else {
            returnValue = CommandUtility.isCommandAvailable("rar");
        }
        return returnValue;
    }

    public static boolean isRarAvailable() {
        return isRarAvailable(null);
    }
}
