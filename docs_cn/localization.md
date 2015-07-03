AcademyCraft本地化格式规范
===

本规范适用于AcademyCraft项目本地化语言命名空间制定和维护、补完本地化语言文件内容用。


一、本地化文件格式规范
---

在开发环境下，只保留en_US.lang一个本地化语言文件，其他语言文件的更新仅在**发布版本**更新。本地化语言命名空间格式如下：
`[modid].[type].[locName]`
其中`[modid]`为ModID常量的值或Mod的通用缩写（如不知道ModID常量是什么，可简单理解为一个Mod的内部名称），`[type]`为本地化语言所属的类型，`[locName]`为本地化语言的内容。
AC项目的`[modid]`值为`ac`，即每条本地化语言命名空间以`ac.`开头。现阶段，`[type]`现阶段可取的值有：

*   gui GUI界面内的文字或提示。使用时请将`[locName]`的值以`[guiName].[fieldName]`（GUI名称.字段名称）的格式表示。
*   `[abilityName]` 能力相关内容。此处的`[abilityName]`值为能力系的简写。如电击使应使用`em`为值。后接`.desc`为能力说明。
*   `[skillName]` 技能相关内容。此处的`[skillName]`值为技能的简写。如矿物探测应使用`minedet`为值。后接`.desc`为技能说明。
*   hud HUD界面的文字或提示。要求同gui。
*   app 数据终端APP的相关内容。
本列表可能不完整，将在开发过程中补全。
撰写时请将字段按照一定的规则分类放置，加以注释（以`#`开头的行）以区分字段所涉及的内容。

二、调用本地化字段规范
---

默认使用`cn.academy.client.ACLang`类进行本地化字段的获取，请维护本类的开发者将调用方法分类放置，并撰写相关注释，以便于维护。
暂行规范方法名为以下格式`[type][fieldName]`，例如`txtFileNotFound`代表File Not Found（文件未找到）的本地化字段。
在维护该类的过程中请注意代码美观与维护便利。


三、其他注意
---

无论是维护本地化.lang文件还是维护ACLang类，都需要各位对字段整理和维护的排序与排版的认识。在一次对LIUtils CGUI包的Localization作业中，
作者发现坑爹的本地化加载是不支持空白占位符（半角空格）的，失去了一个使本地化字段有分层排序的方式。但是这点更加加重了使用更好规范的重要性。
在该Localization作业中，我使用的格式规范如下。

```
# LIUtils Language File

# CGUI
cgui.command.usage=/cgui <filepath> or /cgui
cgui.command.filenotfound=File doesnt exist.

cgui.text.selection=Current selection: 
cgui.text.background=Background

cgui.gui.toolbar=Toolbar
cgui.button.save=Save
cgui.button.saveAs=Save As
cgui.button.addWidget=Add Widget
cgui.button.hierarchy=Hierarchy

cgui.gui.hierarchy=Hierarchy
cgui.button.child=Become Child
cgui.button.dechild=De-child
cgui.button.moveUp=Move Up
cgui.button.moveDown=Move Down
cgui.button.remove=Remove
cgui.button.duplicate=Duplicate
cgui.button.rename=Rename

cgui.gui.componentEditor=Component: 
```

CGUILang看起来是这样的
```java
package cn.liutils.cgui.client;

import net.minecraft.util.StatCollector;

public class CGUILang {
	
	public static String guiComeditor() {
		return local("cgui.gui.componentEditor");
	}
	
	public static String txtBackground() {
		return local("cgui.text.background");
	}
	
	public static String txtSelection() {
		return local("cgui.text.selection");
	}
	
	public static String butRename() {
		return local("cgui.button.rename");
	}
	
	public static String butDuplicate() {
		return local("cgui.button.duplicate");
	}
	
	public static String butRemove() {
		return local("cgui.button.remove");
	}
	
	public static String butMoveDown() {
		return local("cgui.button.moveDown");
	}
	
	public static String butMoveUp() {
		return local("cgui.button.moveUp");
	}
	
	public static String butChild() {
		return local("cgui.button.child");
	}
	
	public static String butDechild() {
		return local("cgui.button.dechild");
	}
	
	public static String butHierarchy() {
		return local("cgui.button.hierarchy");
	}
	
	public static String butAdd() {
		return local("cgui.button.addWidget");
	}
	
	public static String butSaveAs() {
		return local("cgui.button.saveAs");
	}
	
	public static String butSave() {
		return local("cgui.button.save");
	}
	
	public static String guiHierarchy() {
		return local("cgui.gui.hierarchy");
	}
	
	public static String guiToolbar() {
		return local("cgui.gui.toolbar");
	}
	 
	public static String commFileNotFound() {
		return local("cgui.command.filenotfound");
	}
	
	public static String commUsage() {
		return local("cgui.command.usage");
	}
	 
	private static String local(String str) {
		return StatCollector.translateToLocal(str);
	}
	
}
```

效果不错，这个设计方式可以作为参照。
另外，在调用字段的时候尽量使用同一含义相近用处的同一字段，以减少本地化文件的复杂程度。

KS