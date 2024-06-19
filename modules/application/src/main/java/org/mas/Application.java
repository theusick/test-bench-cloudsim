package org.mas;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Application {

    private final CloudSimPlus simulation;
    private final DatacenterBroker broker0;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private Datacenter datacenter0;

    private Properties config;

    public static void main(String[] args) {
        new Application();
    }

    private Application() {
        loadConfig();

        simulation = new CloudSimPlus();
        datacenter0 = createDatacenter();
        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new DatacenterBrokerSimple(simulation);

        vmList = createVms();
        cloudletList = createCloudlets();
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);

        simulation.start();

        final var cloudletFinishedList = broker0.getCloudletFinishedList();
        new CloudletsTableBuilder(cloudletFinishedList).build();
    }

    /**
     * Loading parameters from config file
     */
    private void loadConfig() {
        config = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private Datacenter createDatacenter() {
        int hosts = Integer.parseInt(config.getProperty("hosts"));
        final var hostList = new ArrayList<Host>(hosts);
        for (int i = 0; i < hosts; i++) {
            final var host = createHost();
            hostList.add(host);
        }
        return new DatacenterSimple(simulation, hostList);
    }

    private Host createHost() {
        int hostPes = Integer.parseInt(config.getProperty("host.pes"));
        int hostMips = Integer.parseInt(config.getProperty("host.mips"));
        int hostRam = Integer.parseInt(config.getProperty("host.ram"));
        long hostBw = Long.parseLong(config.getProperty("host.bw"));
        long hostStorage = Long.parseLong(config.getProperty("host.storage"));

        final var peList = new ArrayList<Pe>(hostPes);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < hostPes; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(hostMips));
        }

        return new HostSimple(hostRam, hostBw, hostStorage, peList);
    }

    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms() {
        int vms = Integer.parseInt(config.getProperty("vms"));
        int vmPes = Integer.parseInt(config.getProperty("vm.pes"));
        int vmRam = Integer.parseInt(config.getProperty("vm.ram"));
        int vmBw = Integer.parseInt(config.getProperty("vm.bw"));
        int vmSize = Integer.parseInt(config.getProperty("vm.size"));

        final var vmList = new ArrayList<Vm>(vms);
        for (int i = 0; i < vms; i++) {
            final var vm = new VmSimple(vmRam, vmPes);
            vm.setRam(vmRam).setBw(vmBw).setSize(vmSize);
            vmList.add(vm);
        }
        return vmList;
    }

    /**
     * Creates a list of Cloudlets.
     */
    private List<Cloudlet> createCloudlets() {
        int cloudlets = Integer.parseInt(config.getProperty("cloudlets"));
        int cloudletPes = Integer.parseInt(config.getProperty("cloudlet.pes"));
        int cloudletLength = Integer.parseInt(config.getProperty("cloudlet.length"));
        int cloudletSize = Integer.parseInt(config.getProperty("cloudlet.size"));
        double utilization = Double.parseDouble(config.getProperty("utilization"));

        final var cloudletList = new ArrayList<Cloudlet>(cloudlets);
        final var utilizationModel = new UtilizationModelDynamic(utilization);

        for (int i = 0; i < cloudlets; i++) {
            final var cloudlet = new CloudletSimple(cloudletLength, cloudletPes, utilizationModel);
            cloudlet.setSizes(cloudletSize);
            cloudletList.add(cloudlet);
        }
        return cloudletList;
    }
}
