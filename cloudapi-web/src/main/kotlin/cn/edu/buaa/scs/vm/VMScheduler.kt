package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.controller.models.VirtualMachine
import cn.edu.buaa.scs.kube.crd.v1alpha1.VirtualMachineSpec
import kotlin.random.Random
import kotlin.math.sqrt

class Chromosome(private val vmList: List<VirtualMachineSpec>, private val hostList: List<PhysicalHost>) {
    private var allocation: IntArray // 该染色体的基因（一种分配方案）
    private var fitnessValue: Double = 0.0 // 适应度值，表示该染色体的优劣程度

    init {
        // 随机初始化分配方案
        this.allocation = IntArray(vmList.size) { Random.nextInt(hostList.size) } // 分配方案，每个元素表示对应虚拟机分配给哪个主机
        evaluateFitness() // 计算适应度值
    }

    constructor(allocation: IntArray, vmList: List<VirtualMachineSpec>, hostList: List<PhysicalHost>) : this(vmList, hostList) {
        this.allocation = allocation
        evaluateFitness()
    }

    // 交叉操作，返回生成的新染色体
    fun crossWith(other: Chromosome): Chromosome {
        val length = allocation.size
        val cutPoint = (Math.random() * (length - 1)).toInt() + 1 // 随机选择一个交叉点，保证至少有一个基因片段来自每个染色体
        val newAllocation = allocation.copyOf() // 复制当前染色体的基因

        for (i in cutPoint until length) {
            newAllocation[i] = other.allocation[i] // 将当前染色体的后半部分替换为另一个染色体的后半部分
        }
        return Chromosome(newAllocation, vmList, hostList)
    }

    // 单点变异操作，返回生成的新染色体
    fun mutate() {
        // 随机选择一个虚拟机，并重新随机分配它的主机
        val vmIndex = Random.nextInt(vmList.size)
        allocation[vmIndex] = Random.nextInt(hostList.size)
        evaluateFitness()
    }

    // 计算各主机CPU/内存利用率%：U(i)CPU, U(i)RAM
    // 所有主机的CPU/内存利用率均值%：U(avg)CPU = ∑U(i)CPU/n, U(avg)RAM = ∑U(i)RAM/n
    // 计算CPU/内存的利用均衡度（样本标准差）：D = sqrt (1/n)*∑[(U(i)-U(avg))^2]
    // 适应度值 = a*Dcpu + b*Dram
    private fun evaluateFitness() {
        val n = allocation.size // 总共虚拟机个数
        val hosts = mutableListOf<PhysicalHost>() // 用于统计每个主机的资源利用情况
        val cpuUsages = mutableListOf<Double>() // 存储每个主机的 CPU 利用率
        val ramUsages = mutableListOf<Double>() // 存储每个主机的内存利用率

        hostList.forEach { // 建立临时的主机列表用于本次计算
            hosts.add(it.copy())
        }

        // 遍历所有的虚拟机，将其占用资源叠加到对应的物理主机上
        for (i in 0 until n) {
            val hostIndex = allocation[i] // host在hostList里的下标
            val vmCpu = vmList[i].cpu
            val vmRam = vmList[i].memory
//            val vmDisk = vmList[i].diskSize

            // 更新主机的资源占用情况
            hosts[hostIndex].cpu_used_mhz += vmCpu * 10 // vmCpu是虚拟机CPU内核数，只能近似转换为mhz
            hosts[hostIndex].memory_used_mb += vmRam
//            host.disk += vmDisk
        }

        // 计算每个主机的 CPU&内存 利用率
        for (host in hosts) {
            cpuUsages.add(host.cpu_used_mhz.toDouble() / host.cpu_total_mhz)
            ramUsages.add(host.memory_used_mb.toDouble() / host.memory_total_mb)
        }

        // 计算所有主机的CPU/内存利用率均值
        val avgCpuUsage = cpuUsages.average()
        val avgRamUsage = ramUsages.average()

        // 计算CPU/内存的利用均衡度（样本标准差）
        val cpuStd = sqrt(cpuUsages.sumOf { (it - avgCpuUsage) * (it - avgCpuUsage) } / n)
        val ramStd = sqrt(ramUsages.sumOf { (it - avgRamUsage) * (it - avgRamUsage) } / n)

        // 根据给定的参数计算适应度值
        val a = 0.6
        val b = 0.4
        val fitness = 2 - (a * cpuStd + b * ramStd)
        this.fitnessValue = fitness
    }

    // 获取染色体的分配方案
    fun getAllocation(): IntArray {
        return allocation
    }

    fun getFitness(): Double {
        return fitnessValue
    }
}


class GeneticAlgorithm(private val vmList: List<VirtualMachineSpec>, private val hostList: List<PhysicalHost>, private val populationSize: Int,
                       private val mutationRate: Double, private val crossoverRate: Double, private val elitismCount: Int, private val evolveCycles: Int) {

    private var population: MutableList<Chromosome> = mutableListOf()

    init {
        for (i in 0 until populationSize) {
            population.add(Chromosome(vmList, hostList))
        }
    }

    fun evolve() {
        repeat(evolveCycles) {
            var newPopulation: MutableList<Chromosome> = mutableListOf() // 下一代种群
            // elitism，选择适应度最大的（精英）生成下一代种群
            for (i in 0 until elitismCount) {
                val elite = population.maxByOrNull { it.getFitness() }!!
                newPopulation.add(elite)
            }

            // crossover，交叉【拷贝突变】
            while (newPopulation.size < populationSize) {
                val parent1 = selectParent()
                val parent2 = selectParent()

                val child1 = parent1.crossWith(parent2)
                val child2 = parent2.crossWith(parent1)

                if (Math.random() < crossoverRate) { // 以某概率执行条件
                    newPopulation.add(child1)
                }
                if (Math.random() < crossoverRate) {
                    newPopulation.add(child2)
                }
            }

            // mutation，突变【原地突变】
            for (i in elitismCount until newPopulation.size) {
                if (Math.random() < mutationRate) {
                    newPopulation[i].mutate()
                }
            }

            // 新种群替换旧种群
            population = newPopulation
        }
    }

    fun getBestSolution(): Chromosome {
        return population.maxByOrNull { it.getFitness() }!!
    }

    // 轮盘赌法选择可以遗传下一代的染色体
    private fun selectParent(): Chromosome {
        var sum = 0.0
        for (chromosome in population) {
            sum += chromosome.getFitness()
        }
        var rouletteWheelPosition = Math.random() * sum
        for (chromosome in population) {
            rouletteWheelPosition -= chromosome.getFitness()
            if (rouletteWheelPosition <= 0) {
                return chromosome
            }
        }
        return population.last()
    }
}

//    class Chromosome(var genes: List<Int>) {
//
//        constructor() : this(List(NUMBER_OF_VIRTUAL_MACHINES) { Random.nextInt(NUMBER_OF_PHYSICAL_MACHINES) })
//
//        constructor(parent1: Chromosome, parent2: Chromosome) {
//            val crossoverPoint = Random.nextInt(NUMBER_OF_VIRTUAL_MACHINES)
//            genes = parent1.genes.take(crossoverPoint) + parent2.genes.drop(crossoverPoint)
//        }
//
//        var fitness: Double = 0.0
//            private set
//
//        init {
//            calculateFitness()
//        }
//
//        fun mutate() {
//            val index = Random.nextInt(NUMBER_OF_VIRTUAL_MACHINES)
//            genes = genes.toMutableList().apply { set(index, Random.nextInt(NUMBER_OF_PHYSICAL_MACHINES)) }
//            calculateFitness()
//        }
//
//        fun evaluateFitness() {
//            // 获取所有主机的资源利用率
//            val hostUsage = platform.getHostUsage()
//
//            // 计算所有CPU、内存、磁盘的平均利用率
//            val cpuAvg = hostUsage.map { it.cpuUsage }.average()
//            val memoryAvg = hostUsage.map { it.memoryUsage }.average()
//            val diskAvg = hostUsage.map { it.diskUsage }.average()
//
//            // 计算CPU、内存、磁盘的利用率方差
//            val cpuVariance = hostUsage.map { it.cpuUsage }.let { usage ->
//                usage.map { (it - cpuAvg).pow(2) }.sum() / usage.size
//            }
//            val memoryVariance = hostUsage.map { it.memoryUsage }.let { usage ->
//                usage.map { (it - memoryAvg).pow(2) }.sum() / usage.size
//            }
//            val diskVariance = hostUsage.map { it.diskUsage }.let { usage ->
//                usage.map { (it - diskAvg).pow(2) }.sum() / usage.size
//            }
//
//            // 计算负载均衡度和节点间资源平衡度的加权平均值
//            val loadBalanceWeight = 0.5 // 负载均衡度权重
//            val resourceBalanceWeight = 0.5 // 资源平衡度权重
//            val loadBalance = 1 - (cpuVariance + memoryVariance + diskVariance) / 3
//            val resourceBalance = (hostUsage.map { abs(it.cpuUsage - cpuAvg) }.sum() +
//                    hostUsage.map { abs(it.memoryUsage - memoryAvg) }.sum() +
//                    hostUsage.map { abs(it.diskUsage - diskAvg) }.sum()) / (3 * numHosts)
//            val fitness = loadBalanceWeight * loadBalance + resourceBalanceWeight * resourceBalance
//
//            this.fitness = fitness
//        }
//    }
//}
