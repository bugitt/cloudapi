package cn.edu.buaa.scs.kube.crd.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Group(Constants.GROUP)
@Version(Constants.API_VERSION)
@Kind("VirtualMachine")
public class VirtualMachine extends CustomResource<VirtualMachineSpec, VirtualMachineStatus> implements Namespaced {

}
