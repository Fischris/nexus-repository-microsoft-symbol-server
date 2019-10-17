package org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted.pdb_file.intern;

import org.sonatype.nexus.repository.storage.TempBlob;

import java.io.*;

public class ReadBytes {




    public static byte[] readNBytes(TempBlob tempBlob, int startindex, int lengthN){

        InputStream is = tempBlob.getBlob().getInputStream();

        byte[] bArray = new byte[lengthN];
        try{
            is.skip(startindex);
            is.read(bArray);

        }catch(IOException ioExp){
            ioExp.printStackTrace();
        }
        return  bArray;
    }
}
