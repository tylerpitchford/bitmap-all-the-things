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

/**
 * Created by tpitchford on 6/23/15.
 */
public class Main {

    public static void main(String[] args) {
        if(args.length == 0) {
            //assume we wanted the gui
            BattEngineGUI.launch();
        } else {
            try {
                new BattEngine().start(args);
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

}
