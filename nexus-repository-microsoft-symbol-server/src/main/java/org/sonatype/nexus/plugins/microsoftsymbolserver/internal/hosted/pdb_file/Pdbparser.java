package org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted.pdb_file;

import org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted.pdb_file.data.PdbData;

import org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted.pdb_file.intern.DataSearcher;
import org.sonatype.nexus.repository.storage.TempBlob;



public class Pdbparser {


    PdbData pdbData = new PdbData();




    public Pdbparser() {
    }



    public String getGuidWithAge(TempBlob tempBlob) throws ParseException{

        String retval;

        DataSearcher dataSearcher = new DataSearcher();

        try{

            if (!dataSearcher.checkIfFileIsPdb(tempBlob)){
                throw new ParseException("Is no Pdb-File!");
            }

            dataSearcher.doSuperBlock(tempBlob, pdbData);
            dataSearcher.dofirstStreamDirBlockLocation(tempBlob, pdbData);
            dataSearcher.doPdbStreamHeaderLocation(tempBlob, pdbData);
            dataSearcher.doPdbStreamHeader(tempBlob, pdbData);
            retval = pdbData.data_guid + pdbData.data_age;

        }catch (Exception e){
            throw new ParseException("Error during parsing file:" + e.toString());
        }
        return retval.toUpperCase();
    }


}
