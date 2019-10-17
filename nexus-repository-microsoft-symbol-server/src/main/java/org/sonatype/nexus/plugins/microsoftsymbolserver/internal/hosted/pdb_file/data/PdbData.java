package org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted.pdb_file.data;

public class PdbData {


    /********************************
     * this is part of the PdbStream*/
    public int headerLocation;
    //public int data_version; not needed
    //public int data_sig;  not needed
    public int data_age;
    public String data_guid;
    /********************************/


    /********************************
     *  this block is part of the StreamDirLocation*/
    public int firstBlockStreamDir;
    /********************************/


    /********************************
     *  this block is part of the SuperBlock*/
    public int BlockSize;
    // public long FreeBlockMapBlock; - not necessary
    // public int NumBlocks; - not necessary
    // public long NumDirectoryBytes; - not necessary
    // public long Unknown; - not necessary
    public int BlockMapAddr;
    /********************************/

}
