package cn.edu.buaa.scs.vcenter

import com.vmware.photon.controller.model.adapters.vsphere.util.connection.Connection
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.GetMoRef
import com.vmware.vim25.ManagedObjectReference
import com.vmware.vim25.RetrieveOptions
import com.vmware.vim25.VirtualMachineTicket

fun Connection.getMoRef(): GetMoRef {
    return GetMoRef(this)
}

fun Connection.getDatacenterRef(): ManagedObjectReference {
    return vimPort.findByInventoryPath(serviceContent.searchIndex, "Datacenter")
}

fun Connection.getCreateVmSubFolder(): ManagedObjectReference {
    val getMoRef = getMoRef()
    val vmFolderRef = getMoRef.entityProps(getDatacenterRef(), "vmFolder")["vmFolder"] as ManagedObjectReference
    return getMoRef.inContainerByType(vmFolderRef, "Folder", RetrieveOptions())["other"]
        ?: vimPort.createFolder(vmFolderRef, "other")
}

fun Connection.getVmRefByUuid(uuid: String): ManagedObjectReference? {
    return vimPort.findByUuid(
        serviceContent.searchIndex,
        getDatacenterRef(),
        uuid,
        true,
        false
    )
}
