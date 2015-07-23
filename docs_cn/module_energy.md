.# AcademyCraft 标准文档
## energy模块

energy模块是AC的无线能源网部分。它同时包含着默认的能源网支持内容（矩阵、节点、三种基本发电机、物品等）以及完整的无线能源API。
AC中的能源系统以IF作为它的独立能源单位。

以下文档将忽略EN的内部实现，仅介绍API部分。




无线能源网
---

无线能源网由以下几种类型的成员方块组成。

* 矩阵(Matrix)：没有存电能力。它的功能是将一定距离以内的节点连接起来并在它们之间进行能源平衡。可以用矩阵来构建大型的能源网络。
* 节点(Node)：有存电能力。每个节点可以连接到一个矩阵（所代表的SSID），并且被发电机和用电器所连接。
* 发电机(Generator): 可以连接到一个节点，向节点提供电力。
* 用电器(Receiver): 可以连接到一个节点，从节点吸取电力。

它们的对应接口如下：

* 矩阵——IWirelessMatrix <- IWirelessTile
* 节点——IWirelessNode <- IWirelessTile
* 发电机——IWirelessGenerator <- IWirelessUser
* 用电器——IWirelessReceiver <- IWirelessUser

发电机和用电器被统称为用户（User）。

你应该在方块的TileEntity类上实现这些接口。

无线能源网的链接分为以下两类：矩阵网络和节点网络。

矩阵网络：以一个矩阵为核心，多个节点连接到该网络。该网络必须由用户手动初始化，提供一个世界唯一的SSID和该能源网的登录
密码。所有节点要连接到该网络，除了要满足节点在矩阵的信号范围内以外，还必须提供正确的密码。

节点网络：以一个节点为核心，多个发电机或用电器连接到该网络。该网络不需要显式的被加载，只要节点存在，该网络就会在
发电机和用电器连接时自动被创建。



能源网使用——操纵
---

无线能源部分的API的实现和接口通过事件系统完全去耦。你不需要访问任何方法来进行对能源网连接的操纵。取而代之，你应该
在MinecraftForge.EVENT_BUS发出一个在`energy.api.event`包中的事件来进行对能源网连接的操纵。
由于一些实现的历史原因，操作的成功与否会通过该event是否canceled表示。如果该event被cancel了，那么代表该操作不成功。

所有和能源网相关的操作都**必须在服务端**进行。如果你尝试在客户端调用能源网部分的API，你会得到一个崩溃。

矩阵网络相关事件（`energy.api.event.wen`）

* `CreateNetworkEvent`： 创建网络
* `DestroyNetworkEvent`： 摧毁网络
* `LinkNodeEvent`： 连接节点
* `UnlinkNodeEvent`： 断开节点连接
* `ChangePassEvent`： 更换密码

节点网络相关事件（`energy.api.event.node`）

* `LinkUserEvent`： 创建连接
* `UnlinkUserEvent`： 断开连接



能源网使用——查询
---

对于当前能源网结构的查询操作通过`energy.api.WirelessHelper`类来进行。

WirelessHelper可以通过特定的查询条件返回一个或多个WirelessNet或NodeConn对象，你可以分别通过它们查询矩阵网络和节点网络
的连接细节信息，但是不可以通过它们更改能源网状态。你还可以使用WirelessHelper查询各个方块在能源网中的连接状态，以及
查询在一个位置附近的无线网络等。

关于其暴露的接口请参考javadoc，在此不再赘述。