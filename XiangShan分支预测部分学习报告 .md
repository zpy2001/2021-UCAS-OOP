# XiangShan分支预测部分学习报告

tag: homework
姓名: 周鹏宇
学号: 2019K8009929039

# 导言

## 关于香山CPU

香山是一款开源的高性能 RISC-V 处理器，基于 Chisel 硬件设计语言实现，支持 RV64GC 指令集。在香山处理器的开发过程中，使用了包括 Chisel、Verilator、GTKwave 等在内的大量开源工具，实现了差分验证、仿真快照、RISC-V 检查点等处理器开发的基础工具，建立起了一套包含设计、实现、验证等在内的基于全开源工具的处理器敏捷开发流程。

## 关于chisel

![chisel_1024.png](XiangShan%E5%88%86%E6%94%AF%E9%A2%84%E6%B5%8B%E9%83%A8%E5%88%86%E5%AD%A6%E4%B9%A0%E6%8A%A5%E5%91%8A%205e721eb9894d4576916a9714bb88af26/chisel_1024.png)

Chisel(Constructing Hardware In a Scala Embedded Language)是一门以Scala为宿主语言开发的硬件构建语言，它是由加州大学伯克利分校的研究团队发布的一种新型硬件语言。

相较于传统的硬件设计语言（比如Verilog），chisel的优势在于：

- 在硬件电路设计中引入了面向对象的特性

![chisel_class.png](XiangShan%E5%88%86%E6%94%AF%E9%A2%84%E6%B5%8B%E9%83%A8%E5%88%86%E5%AD%A6%E4%B9%A0%E6%8A%A5%E5%91%8A%205e721eb9894d4576916a9714bb88af26/chisel_class.png)

chisel的数据类型，其中箭头指向者为父类或者超类（绿色模块为class，红色模块为object，蓝色则为trait）

- 优化了部分Verilog的语法，减少了不必要的部分。这具体体现在一些Verilog可能会出现可以通过仿真（testbench）但无法生成比特流（bitstream）的语法结构，如果参与过国科大的体系结构研讨课或者组成原理研讨课应该会对此深有体会，以笔者自身经历为例，前几日在做异常支持的工作时，出现了一处混合使用上升沿和下降沿触发的触发器，最后可以通过仿真，但在综合时却出现了“组合逻辑环”这一致命错误。而chisel在转换成Verilog语言（这是必要的，因为目前没有直接支持chisel的EDA）时，会**避免出现不可综合的语法。**
- 利用Scala的模式匹配、特质混入、类继承等特性，能够迅速改变电路结构

基于以上特点，相较于传统开发，利用chisel开发要具有**更高的效率**和**更少的代码量**。

## 关于分支预测模块

### 关于分支预测

分支预测技术是**提高通用处理器性能**的重要方法，本质是克服指令控制相关，提高指令并行度，从而使得处理器的性能得到提高。其重要性可以由以下几点来说明：

- 现在的通用处理器大多采用深度流水线和宽发射机制，两者背后分支预测是关键技术支撑。
- 计算机和计算器最大的区别就在于计算机有了分支指令，使得计算机从简单的数字计算转变为完成各种任务和运算。
- 分支预测技术不仅在高性能通用处理器中采用，而且在嵌入式处理器也广泛采用。

### 关于香山中的分支预测模块

香山中的分支预测模块是整个前端的重要组成部分，其通过接受并分析当前PC以及历史信息（His），给出预测结果并最终传递给取指令模块。在这里给出目前香山前端的组成：

- frontend
    - Bim
    - BPU
    - Composer（Class）
    - Frontend
    - FrontendBundle
    - FTB
    - Ibuffer
    - ICache
    - IFU
    - ITTAGE
    - local
    - NewFtq
    - PreDecode
    - RAS
    - SC
    - Tage
    - uBTB
    
    其中，本学习报告重点阅读的将是Bim、BPU、Composer、FTB、NewFtq、RAS、SC和uBTB部分。
    
    当然，单纯如同报菜名一样列出文件组成是毫无意义的（除了水一水页数），为了进一步说明香山CPU分支预测部分的数据传递和面向对象思想，我将以BPU中的Predictor类为例，简单介绍一下分支预测过程中各个部件的配合。
    
    ```scala
    class Predictor(implicit p: Parameters) extends XSModule with HasBPUConst {
      val io = IO(new PredictorIO)
    
      val predictors = Module(if (useBPD) new Composer else new FakePredictor)
    ......
    }
    ```
    
    可以注意到，Predictor类继承了抽象类`XSMoudle`（其实基本上等价于chisel给出的`Moudle`类，但多了一层封装；至于chisel给出的`Moudle`类则是用于帮助工程师定义硬件模块的，一切硬件模块都是继承自`Moudle`类的），同时混入了特质（有点类似单例对象，由于Scala没有多重继承而提出的一种解决方案）`HasBPUConst`：
    
    ```scala
    trait HasBPUConst extends HasXSParameter with HasIFUConst {
      val MaxMetaLength = 1024 // TODO: Reduce meta length
      val MaxBasicBlockSize = 32
      val LHistoryLength = 32
      val numBr = 2
      val useBPD = true
      val useLHist = true
      val shareTailSlot = true
      val numBrSlot = if (shareTailSlot) numBr-1 else numBr
      val totalSlot = numBrSlot + 1
      ......
    }
    ```
    
    可以注意到，该特质的主要用处是定义一些与BPU相关的变量（可以注意到`useBPU`为`true`），那么回到`Predictor`类，其后的`val io = IO(new PredictorIO)` 是定义这个模块的接口，具体可以参考`PredictorIO` 并继续向上追索，本处不予以赘述（追索线条过长，而且由于不是以继承的形式传递，因此很大程度上只能面向代码追索）。
    
    随后，其例化了一个模块（用面向对象的语言描述，就是实例化了一个类），该模块为`composer` （因为`useBPU`必然是`true`），因而转到`composer`类进行分析。
    
    ```scala
    class Composer(implicit p: Parameters) extends BasePredictor with HasBPUConst {
      val (components, resp) = getBPDComponents(io.in.bits.resp_in(0), p)
      io.out.resp := resp
      ......
    }
    ```
    
    在这里我们注意到，其调用了`getBPDComponents` 方法，故而继续追索，此时用UML图简单观察：
    
    ![Composer.png](XiangShan%E5%88%86%E6%94%AF%E9%A2%84%E6%B5%8B%E9%83%A8%E5%88%86%E5%AD%A6%E4%B9%A0%E6%8A%A5%E5%91%8A%205e721eb9894d4576916a9714bb88af26/Composer.png)
    
    composer类的UML图
    
    而`getBPDComponents` 方法，就在`HasXSParameter`特质中被定义的。
    
    ```scala
    def getBPDComponents(resp_in: BranchPredictionResp, p: Parameters) = {
        coreParams.branchPredictor(resp_in, p)
      }
    ```
    
    可以注意到，其结果又调用了`XScoreParameters`样例类（在`HasXSParameter`中被实例化）中的方法`branchPredictor`，进而继续追索，到达本次旅途的终点站：
    
    ```scala
    branchPredictor: Function2[BranchPredictionResp, Parameters, Tuple2[Seq[BasePredictor], BranchPredictionResp]] =
        ((resp_in: BranchPredictionResp, p: Parameters) => {
          // val loop = Module(new LoopPredictor)
          // val tage = (if(EnableBPD) { if (EnableSC) Module(new Tage_SC)
          //                             else          Module(new Tage) }
          //             else          { Module(new FakeTage) })
          val ftb = Module(new FTB()(p))
          val ubtb = Module(new MicroBTB()(p))
          val bim = Module(new BIM()(p))
          val tage = Module(new Tage_SC()(p))
          val ras = Module(new RAS()(p))
          val ittage = Module(new ITTage()(p))
          // val tage = Module(new Tage()(p))
          // val fake = Module(new FakePredictor()(p))
    
          // val preds = Seq(loop, tage, btb, ubtb, bim)
          val preds = Seq(bim, ubtb, tage, ftb, ittage, ras)
          preds.map(_.io := DontCare)
    
          // ubtb.io.resp_in(0)  := resp_in
          // bim.io.resp_in(0)   := ubtb.io.resp
          // btb.io.resp_in(0)   := bim.io.resp
          // tage.io.resp_in(0)  := btb.io.resp
          // loop.io.resp_in(0)  := tage.io.resp
          bim.io.in.bits.resp_in(0)  := resp_in
          ubtb.io.in.bits.resp_in(0) := bim.io.out.resp
          tage.io.in.bits.resp_in(0) := ubtb.io.out.resp
          ftb.io.in.bits.resp_in(0)  := tage.io.out.resp
          ittage.io.in.bits.resp_in(0)  := ftb.io.out.resp
          ras.io.in.bits.resp_in(0) := ittage.io.out.resp
    
          (preds, ras.io.out.resp)
        })
    ```
    
    在这里我们可以了解到分支预测部件和分支预测整体模块关系的一隅：在这里，各种分支预测部件被实例化（比如Bim、ITTage等），他们的结果则是由**覆盖重定向**得出的。这里简单介绍一下不同部件的作用：
    
    - bim：进行方向预测
    - ubtb：存储跳转地址并进行跳转
    - tage：更先进（准确）的方向预测
    - ittage：处理间接跳转（比如jalr）
    - ras：也是处理间接跳转，可以处理syscall/ertn等，正确率相当高
    
    当多级预测产生不同的历史结果时，用后级覆盖并冲刷前级流水线，从而保证每次预测使用的历史准确，进而提高预测成功率。
    

至于分支预测部分在整个前端中的体现，则体现在`frounted`文件中对其的实例化：

```scala
//decouped-frontend modules
  val bpu     = Module(new Predictor)
  val ifu     = Module(new NewIFU)
  val ibuffer =  Module(new Ibuffer)
  val ftq = Module(new Ftq)
```

## 导言小结

导言部分重点介绍了香山CPU开源项目，chisel语言以及分支预测部分，并以BPU中的Predictor为例管窥蠡测此项目的类、对象、实例化、继承和封装。

其实可以注意到，各个模块间的协同配合很少使用**继承**语法，被继承最多的往往是常用参数的声明类或特质（比如`HasXSParameter`），其余大部分时候都是会选择使用例化（实例化）等语法，这不仅满足开放封闭原则、高内聚低耦合等面向对象要求的原则，也符合硬件设计上的习惯，同时，硬件设计的要求又使之必须满足接口功能单一、接口隔离原则（毕竟如果BPU能随便把ICatch内容修改一番，那混乱的指令足以立刻使CPU宕机）。

简言之，无论是chisel的语法设计还是香山项目对其的运用，都无时无刻不在硬件设计思路的影响下体现着面向对象的宗旨：**抽象和封装**。

```scala
import chisel3._

Class Open_XiangShan extends OOP with FPGA{}
```

香山CPU是个仍在不断进步的开源项目，事实上，笔者阅读的时候甚至还有幸多次目击了项目组git push的现场（IDEA突然弹出有新的提交内容，是否git pull），而且目前的架构也已经与在repo中的早期架构图有了不少出入，这一方面增大了我学习和阅读的难度，但另一方面也给了我目睹一个开源项目逐渐成长的机会。在之后的报告中，我将重点基于分支预测部分，学习其代码中体现的面向对象的思想和FPGA的设计思路等。

不过，笔者之前甚少接触面向对象语言，一直以来一直以C和Verilog作为主要的练习对象，两者一个是严格的面向过程语言，另一个虽然有大量的封装，但没有继承和多态，也称不上是面向对象语言，因此只能在阅读中学习，不仅仅是代码的语法和技巧，更重要的是面向对象的思想和设计思路。

最后用一个本次涉及到的所有的类、特质所构成的UML图结束此部分。

![Predictor.png](XiangShan%E5%88%86%E6%94%AF%E9%A2%84%E6%B5%8B%E9%83%A8%E5%88%86%E5%AD%A6%E4%B9%A0%E6%8A%A5%E5%91%8A%205e721eb9894d4576916a9714bb88af26/Predictor.png)

# Part1:香山设计中的类，对象与封装

## 1. 类和对象

## 2.关于继承

## 3.封装与模块——面向对象与FPGA设计的奇妙结合：