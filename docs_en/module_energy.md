# SPECIFICATION DOCUMENTATION OF ACADEMYCRAFT
## MODULE ENERGY

The energy module is the wireless energy network module of AcademyCraft. It also includes the default support content of energy network(the matrix, the node, the three kinds of basic generator and others things) and whole API of wireless energy.
The energy system in AcademyCraft takes IF as its special energy unit.

Here and after wouldn't include all the internal implementations. We only introduce the APIs.




the Wireless Energy Network
---

The wireless energy network is constructed by the following kinds of member block.

* Matrix isn't able to store electricity. Its function is linking the nodes in the certain distance and balance the energy between the nodes. You can use Matrix to construct the huge energy network.          
* Node is able to store electricity. Every nodes can link to a Matrix(its SSID) and be linked by generator and receiver.
* Generator can link to a node and provide electricity to it.
* Receiver can link to a node and draw electricity.

The interfaces to them are as follows:

* Matrix——`IWirelessMatrix` <- `IWirelessTile`
* Node——`IWirelessNode` <- `IWirelessTile`
* Generator——`IWirelessGenerator` <- `IWirelessUser`
* Receiver——`IWirelessReceiver` <- `IWirelessUser`

The generators and receives are collectively referred to as **User**.

You should implement those interfaces in the TileEntity of the corresponding block.

The connection of wireless energy network is divided into the following two categories: **the WEN/Wireless Energy Network** and **the Node Connection**.

The matrix network takes a matrix as the core, and the multiple nodes connect to this network. This network must to be initialized by user, and be provided a world unique SSID and the logic
password of this energy network from user. If any node want to connect to this network, not only need to be in the signal range of matrix, but also need to provide the right password.

Nodes network takes a node as the core, and be connected by multiple generators or receivers. This network needn't to be loaded explicitly. As long as the node exists,
this network will be created automatically when the generators and receivers connect to.



the Usage of the Energy Network: How to Control
---

By using the event system, the interfaces and implementations of the wireless energy network APIs are be decoupling. You needn't to visit any method to control the connection of the energy network.
Instead, you should send a event which is in the package `energy.api.event` to the `MinecraftForge.EVENT_BUS` to control the energy network.
Because of some historical reasons of the implementations, the success of the operation will be expressed by the canceled status of this event. If the event was canceled, the operation is failure.

All the operations about energy network must be conducted in **the server**. If you try to call the APIs of energy network in the client, the game will crash.

Events of the matrix network(`energy.api.event.wen`):

* `CreateNetworkEvent`： Create the network.
* `DestroyNetworkEvent`： Destroy the network.
* `LinkNodeEvent`： Connect to the node.
* `UnlinkNodeEvent`： Disconnect from the node.
* `ChangePassEvent`： Change the password of the matrix network.

Events of the node network(`energy.api.event.node`):

* `LinkUserEvent`： Create the network.
* `UnlinkUserEvent`： Disconnect the network from the node network.



the Usage of the Energy Network: How to Query
---

By using the class `energy.api.WirelessHelper`, you can query the structure of current energy network.

`WirelessHelper` can return one or more instance of `WirelessNet` or `NodeConn`. By using them, you can query the details of the matrix/node network connection respectively, but you can't change the status of energy network by this way. You can also use the WirelessHelper to query the connecting status of each blocks or the wireless network near a position.

关于其暴露的接口请参考javadoc，在此不再赘述。
And please refer to the JavaDoc for the details of public interface. We will not explain more details here.
