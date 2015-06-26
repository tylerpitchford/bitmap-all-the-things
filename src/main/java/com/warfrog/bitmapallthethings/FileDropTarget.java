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

import javax.swing.text.JTextComponent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;

public class FileDropTarget implements DropTargetListener {

    private JTextComponent target = null;

    public FileDropTarget(JTextComponent target) {
        this.target = target;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        //do nothing
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        //do nothing
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        //do nothing
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        //do nothing
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        Transferable transfer = dtde.getTransferable();
        try {
            if(transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                java.util.List<File> list = (java.util.List<File>) transfer.getTransferData(DataFlavor.javaFileListFlavor);
                //only take the first result
                target.setText(list.get(0).getAbsolutePath());
            }
        }
        catch (Exception ex) {
            //ignore
        }
    }
}
