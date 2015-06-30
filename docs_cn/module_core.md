# AcademyCraft 标准文档
# core模块


Author：WeAthFolD

本文档中的所有类路径说明均以 ```cn.academy.core.```起头。

内容说明
---

core包负责AC的核心加载和一些抽象包装。AC的主类就在core包中，在这里也存储一些和内容无关的wrapup和工具。

接下来的内容主要说明core包中现存工具和抽象，以及他们的基本使用方法。细节请参照代码注释。



一般Utils
---

* ```ACSounds```： 播放声音的helper方法。
* ```ControlOverrider```： 对于操作覆盖特别有用的工具。这个类可以临时屏蔽MC的某个原生键位，并且动态的恢复屏蔽。
* ```ModuleCoreClient.keyHandler, dynKeyHandler```： AC全局的KeyHandler管理器。你可以用第一个来注册需要被cfg影响的键位，第二个来注册不需要被cfg影响的键位。




模板类
---

* ```ACItem```： 物品的基类。自动设置物品名称和创造栏。
* ```ACBlock```： 方块基类。自动设置名称和创造栏。
* ```ACBlockContainer```： Container版的方块基类。
* ```ACCommand```： 指令基类。提供了AC的本地化字符串的格式化方法。
* ```TileRecieverBase```： 能量接收方块的Tile的基类。
* ```TileInventory```： 带物品栏的Tile的基类。
* ```EntityRayBase&RenderRayXXX```： 光束和光束渲染。为AC中诸多的光束特效提供的预包装……


注册
---
* ```InstanceEjector``` 用于将JsonLoader里的Block或Item实例自动导出到静态域。
* ```RegACKeyHandler``` 用于注册键位
* ```RegDataPart``` 用于注册DataPart


util.DataPart
---

__DataPart__是用来应对MC自身恶劣的实体附加数据管理而写的组件类。

它的基本概念：每个```DataPart```类是一个可以被静态（永久性）搭载在玩家之上的数据类型，
并且这个数据类型在client和server之间存在着一定的数据同步。```DataPart```为你自动处理好了搭载、创建、从NBT读写的过程，并且为你准备了非常方便
的数据同步方法。如果你需要一些基于玩家的附加数据，使用这个是最好的选择。

DataPart是被```util.PlayerData```所驱动的。你可以通过

```java
PlayerData.register("id", MyDataPart.class);
```

来注册一个DataPart。

当然，你也可以使用AR来注册DataPart，形如：

```java
@Registrant
@RegDataPart("CP")
public class CPData extends DataPart { ... }
```

在被注册之后，DataPart会在玩家进入世界时自动被构建，并且在之后的过程中保证可用。你可以通过```PlayerData.get(player).getDataPart()```来获取DataPart。

DataPart以服务端从NBT所读取的数据为准加载，并且在加载时会将数据同步到客户端。
因此，在客户端存在获取时刻数据尚未同步到的可能性，请务必注意这种情况。在client和server，请使用统一的```fromNBT()```和```toNBT()```方法读写数据。

另外，DataPart已经拥有一个__INSTANCE__序列化器，所以你可以非常方便的使用NetworkCall进行基于它的自定义同步。

最后，推荐在每个自定义DataPart里使用一个```public static get(EntityPlayer)```方法获取对应于玩家的该DataPart实例，可以大幅减少代码量。


ValuePipeline
---

```ValuePipeline```主要用来处理被动技能一类的需求。你可以把它当做一个魔改+极度简化的事件总线。

你可以往ValuePipeline里面pipe一系列的数值（int, float, double）（via ```ValuePipeline.pipeXXX(key, value, parameters...)```，
并且用一个String作为键（频道）以区分不同的数值内容。在一个值被pipe进去以后，ValuePipeline会遍历该键值的Subscriber，
并且对这个值施加修改。在pipe完成后，所获得的返回值就是被所有Subscriber修改以后的值。

你可以通过```ValuePipeline.register(Object)```来注册一个Subscriber对象。里面所有的用```@SubscribePipeline(key)```标记的方法都认为是一个pipeline方法。
一个pipeline方法应该形如：

```java
@SubscribePipeline("someValue")
public [type] pipelineMethod([type] input, [AnyType] par1, [AnyType] par2, ...);
```

也就是说，第一个参数和返回值必须类型相同，并且都是(int, float , double)中的一种。后面的参数是在pipe时候传入的附加参数。
它们应该描述在计算这个值时候的附加条件（例如，使用技能的玩家实例），并且越少越好。
后面的参数和pipe时的参数的数量和类型精确匹配。如果匹配失败，将会在控制台打印一个警告。

你可以通过AcademyCraft.pipeline访问AC的全局pipeline。

**风格提示：请尽量保证pipe时的参数简洁。一般来说，仅给一个EntityPlayer参数就够了。所有代码应该尽可能保证这个单player参数的规范，避免复杂化。**

提示UI
---
我们实现了一个酷炫的提示UI，就像这样：
![](https://raw.githubusercontent.com/LambdaInnovation/AcademyCraft/master/blob/ui0.jpg)  
访问它的方法是NotifyUI。目前是通过主动在NotifyUI类里面侦听事件并且添加显示的方法实现的。如果后面需要notify的内容较多，再考虑开放接口。