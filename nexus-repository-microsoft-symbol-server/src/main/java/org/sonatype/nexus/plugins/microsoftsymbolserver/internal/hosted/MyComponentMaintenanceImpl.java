package org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted;


import java.util.Collections;
import java.util.Set;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

//Asset delete: deletes also folders, if asset is last one.

@Named
public class MyComponentMaintenanceImpl extends DefaultComponentMaintenanceImpl {

    //Deletes also folders, if the deleted asset is the last remaining.
    @Override
    @TransactionalDeleteBlob
    protected Set<String> deleteAssetTx(EntityId assetId, boolean deleteBlob) {
        StorageTx tx = (StorageTx)UnitOfWork.currentTx();
        Asset asset = tx.findAsset(assetId, tx.findBucket(this.getRepository()));
        if (asset == null) {
            return Collections.emptySet();
        } else {
            this.log.info("Deleting asset: {}", asset);


            boolean deleteAlsoComponent = !areThereMoreFiles(tx, asset);             // added:

            tx.deleteAsset(asset, deleteBlob);

            if (deleteAlsoComponent){             // added:

                tx.deleteComponent(tx.findComponent(asset.componentId()));
            }

            return Collections.singleton(asset.name());
        }
    }


    // added to check for more assets
    private boolean areThereMoreFiles(StorageTx tx, Asset asset){

        Iterable<Asset> assets = tx.browseAssets(tx.findComponent(asset.componentId()));
        int counter = 0;
        for (Asset ass : assets){
            counter++;
            if (counter > 1){
                return true;
            }
        }
        return false;
    }

}
