package org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted.pdb_file.intern;

import org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted.pdb_file.data.PdbData;
import org.sonatype.nexus.repository.storage.TempBlob;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class DataSearcher {

    //Fast one, just fetching needed data.
    //There is also a bigger version available, which covers more data like the whole stream dir.



    // Check for Microsoft C/C++ MSF 7.00

    public boolean checkIfFileIsPdb(TempBlob tempBlob){
        byte[] barry = ReadBytes.readNBytes(tempBlob, 0, 32);

        return (getIntFromBinary(barry, 0 ) == 1919117645   &&
                getIntFromBinary(barry, 4 ) == 1718580079   &&
                getIntFromBinary(barry, 8 ) == 792928372    &&
                getIntFromBinary(barry, 12 ) == 539700035   &&
                getIntFromBinary(barry, 16 ) == 541479757   &&
                getIntFromBinary(barry, 20 ) == 808463927   &&
                getIntFromBinary(barry, 24 ) == 1142557197  &&
                getIntFromBinary(barry, 28 ) == 83);
    }


    //short version of reading superblock
    //block size is needed to jump using the block index
    //blockmapadress is index of block which contains adress of stream dir

    public void doSuperBlock(TempBlob tempBlob, PdbData pdbData){


        byte[] barry = ReadBytes.readNBytes(tempBlob, 32, 24);
        pdbData.BlockSize = getIntFromBinary(barry, 0);
        pdbData.BlockMapAddr = getIntFromBinary(barry, 20);
    }

    //reading at blockmapadress block stream dir first block. all others are ignored, cause we need only the first block of the stream dir containing str dir header.

    public void dofirstStreamDirBlockLocation(TempBlob tempBlob, PdbData pdbData){

        byte[] barry = ReadBytes.readNBytes(tempBlob, pdbData.BlockSize * pdbData.BlockMapAddr, 4);
        pdbData.firstBlockStreamDir = getIntFromBinary(barry, 0);
    }


    //Reading part of stream dir to get block index of pdb stream header.
    //numstreams is needed cause we need to know how many byte to skip in the StreamSizes Array
    //firstStreamCountBlocks = Stream index 0 contains n blocks. Needed for skipping.
    //get header location of PdbStream (just 1 block needed, cause 1 block contains header


    public void doPdbStreamHeaderLocation(TempBlob tempBlob, PdbData pdbData){

        byte[] barry = ReadBytes.readNBytes(tempBlob, pdbData.BlockSize * pdbData.firstBlockStreamDir, 8);
        int numStreams = getIntFromBinary(barry, 0);
        int firstStreamCountBlocks = (int) Math.ceil(((double)getIntFromBinary(barry, 4))/pdbData.BlockSize);
        barry = ReadBytes.readNBytes(tempBlob,
                pdbData.BlockSize * pdbData.firstBlockStreamDir + 4 + numStreams * 4 + firstStreamCountBlocks * 4,
                4);
        pdbData.headerLocation = getIntFromBinary(barry, 0);
    }



    //get age and guid to form string adress

    public void doPdbStreamHeader(TempBlob tempBlob, PdbData pdbData){
        byte[] barry = ReadBytes.readNBytes(tempBlob, pdbData.BlockSize * pdbData.headerLocation,  28);
        //pdbStream.data_version = getIntFromBinary(barry, 0); //not really needed
        //pdbStream.data_sig = getIntFromBinary(barry, 4); //not really needed
        pdbData.data_age = getIntFromBinary(barry, 8);
        barry = ReadBytes.readNBytes(tempBlob, pdbData.BlockSize * pdbData.headerLocation + 12,  16);
        pdbData.data_guid = buildGuid(barry);
    }


    //pdb is little endian. all java is big endian.
    private int getIntFromBinary(byte [] barry, int startindex){
        return  ByteBuffer.wrap(barry).order(ByteOrder.LITTLE_ENDIAN).getInt(startindex);
    }

    private String buildGuid(byte[] barry){

        ByteBuffer byteBuffer = ByteBuffer.wrap(barry);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.get(barry);

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("%02x", barry[3]));
        stringBuilder.append(String.format("%02x", barry[2]));
        stringBuilder.append(String.format("%02x", barry[1]));
        stringBuilder.append(String.format("%02x", barry[0]));

        stringBuilder.append(String.format("%02x", barry[5]));
        stringBuilder.append(String.format("%02x", barry[4]));
        stringBuilder.append(String.format("%02x", barry[7]));
        stringBuilder.append(String.format("%02x", barry[6]));

        for (int i = 8; i < barry.length; i++){
            stringBuilder.append(String.format("%02x", barry[i]));
        }

        return stringBuilder.toString();
    }



}
