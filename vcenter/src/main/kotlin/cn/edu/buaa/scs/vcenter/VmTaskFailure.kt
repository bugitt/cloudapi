package cn.edu.buaa.scs.vcenter

import com.vmware.vim25.DuplicateName
import com.vmware.vim25.LocalizedMethodFault

internal class VmNameConflictException(message: String) : RuntimeException(message)

internal fun vmTaskFailure(fault: Any?): Throwable = when (fault) {
    is LocalizedMethodFault -> if (fault.fault is DuplicateName) {
        VmNameConflictException(fault.localizedMessage ?: "Virtual machine name already exists")
    } else {
        RuntimeException(fault.localizedMessage)
    }
    else -> RuntimeException("unknown error")
}
