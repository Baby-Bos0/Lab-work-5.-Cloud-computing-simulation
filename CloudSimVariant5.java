package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;

public class CloudSimVariant5 {

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        // Ініціалізація моделювання для одного користувача.
        CloudSim.init(1, Calendar.getInstance(), false);

        // Один хост із чотирма ядрами по 1000 MIPS.
        List<Pe> peList = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(1000)));
        }

        Host host = new Host(
                new RamProvisionerSimple(8192),
                new BwProvisionerSimple(10000),
                1000000,
                peList,
                new VmSchedulerSpaceShared(peList)
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                        "x86",
                        "Linux",
                        "Xen",
                        hostList,
                        2.0,
                        3.0,
                        0.05,
                        0.001,
                        0.0
                );

        Datacenter datacenter = new Datacenter(
                "Datacenter_Variant5",
                characteristics,
                new VmAllocationPolicySimple(hostList),
                new LinkedList<Storage>(),
                0
        );

        DatacenterBroker broker =
                new DatacenterBroker("Broker_Variant5");

        int brokerId = broker.getId();

        // Чотири однакові віртуальні машини.
        List<Vm> vmList = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Vm vm = new Vm(
                    i,
                    brokerId,
                    1000,   // Продуктивність, MIPS.
                    1,      // Одне віртуальне ядро.
                    1024,   // Оперативна пам'ять, МБ.
                    1000,   // Пропускна здатність.
                    10000,  // Диск, МБ.
                    "Xen",
                    new CloudletSchedulerSpaceShared()
            );

            vmList.add(vm);
        }

        broker.submitGuestList(vmList);

        // Десять однакових запитів.
        List<Cloudlet> cloudletList = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            UtilizationModel utilization =
                    new UtilizationModelFull();

            Cloudlet cloudlet = new Cloudlet(
                    i,
                    10000,  // Довжина завдання, MI.
                    1,      // Потрібне одне ядро.
                    300,    // Розмір вхідних даних, байти.
                    300,    // Розмір вихідних даних, байти.
                    utilization,
                    utilization,
                    utilization
            );

            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }

        broker.submitCloudletList(cloudletList);

        // Розподіл запитів між VM по черзі.
        for (int i = 0; i < 10; i++) {
            broker.bindCloudletToVm(i, i % 4);
        }

        System.out.println("\nVARIANT 5");
        System.out.println("Datacenters: 1");
        System.out.println("Hosts: " + datacenter.getHostList().size());
        System.out.println("Host cores: " + peList.size());
        System.out.println("Virtual machines: " + vmList.size());
        System.out.println("Identical cloudlets: " + cloudletList.size());

        CloudSim.startSimulation();
        CloudSim.stopSimulation();

        // Виведення результатів.
        List<Cloudlet> results =
                broker.getCloudletReceivedList();

        results.sort(
                Comparator.comparingInt(Cloudlet::getCloudletId)
        );

        System.out.println("\n========== RESULTS ==========");
        System.out.printf(
                "%-4s %-10s %-4s %-4s %-10s %-10s %-10s%n",
                "ID", "STATUS", "DC", "VM",
                "CPU TIME", "START", "FINISH"
        );

        int completed = 0;
        double firstStart = Double.POSITIVE_INFINITY;
        double lastFinish = 0;

        for (Cloudlet cloudlet : results) {
            System.out.printf(
                    "%-4d %-10s %-4d %-4d %-10.2f %-10.2f %-10.2f%n",
                    cloudlet.getCloudletId(),
                    cloudlet.getStatus(),
                    cloudlet.getResourceId(),
                    cloudlet.getGuestId(),
                    cloudlet.getActualCPUTime(),
                    cloudlet.getExecStartTime(),
                    cloudlet.getExecFinishTime()
            );

            if (cloudlet.getStatus()
                    == Cloudlet.CloudletStatus.SUCCESS) {
                completed++;
            }

            firstStart = Math.min(
                    firstStart,
                    cloudlet.getExecStartTime()
            );

            lastFinish = Math.max(
                    lastFinish,
                    cloudlet.getExecFinishTime()
            );
        }

        System.out.println("\nCompleted: " + completed + "/10");

        if (!results.isEmpty()) {
            System.out.printf(
                    "Workload duration: %.2f simulated seconds%n",
                    lastFinish - firstStart
            );
        }

        for (Vm vm : vmList) {
            long count = results.stream()
                    .filter(c -> c.getGuestId() == vm.getId())
                    .count();

            System.out.println(
                    "VM " + vm.getId() + ": " + count + " cloudlets"
            );
        }

        if (completed != 10) {
            throw new IllegalStateException(
                    "Not all cloudlets completed successfully"
            );
        }
    }
}