FORMAT SPECIFICATION OF THE LOCALIZATION OF ACADEMYCRAFT
===

This specification applies to you when you are formulating/maintaining/progresssing the localization namespace or the localization language file.


1. Format Specification of the Localization File
---

Under the development enviroment, we only have one localization language file, en_US.lang. We will only update the others language files when we **publish new version**. The naming format of localization language namespace are as follows:
`[modid].[type].[locName]`
In the format, `[modid]` is the value of ModID constant or the universal abbreviation of the Mod. (If you don't know what the ModID constant is, you can simply understand it as a internal name of the Mod). And `[type]` is the type of the localization language. Finally, `[locName]` is the content of the localization language.
The value of `[modid]` of Project AcademyCraft is `ac`. Each localization language namespace begins with `ac.`. At the present stage, you can choose the following values as a `[type]`:

*   `[gui]` is the text or hint on the GUI interface. Please use `[locName]` in the form of `[guiName].[fieldName]`(the name of GUI.the name of field).
*   `[abilityName]` is the content about ability. The value of `[abilityName]` here is the abbreviation of ability department. Such as `em` is electro master. Then the `.desc` after that abbreviation is discription of ability.
*   `[skillName]` is the discription of skill. And the value of `[skillName]` here is the abbreviated form of the skill. For example, you should use `minedet` as a value for Mineral Explorer. And the `.desc` after that is discription of that skill.
*   `[hud]` is the text or hint of HUD. The requirement of this is same as the requirement of GUI.
*   `[app]` is the content about the app of data terminal.
This list may not be complete. We will complete it in development process.
Please classify fields by some certain rules. And add comments(the line begin with `#`) to distinguish the content field involved.

2. the Specification of Localized Field
---

We use the Class `cn.academy.client.ACLang` to get the localized fields by default. In order to maintain easily, the developer who maintain this Class classify the methods and write related comments.
The provisional standard format is like `[type][fieldName]`. For example, `txtFileNotFound` means the localized field of File Not Found.
Please pay attention to facilitate maintenance and artistic of the code when you are maintaining this Class.


3. Anything Else Needs to Be Aware
---

You need to understand something about arranging of fields and sorting/mackuping of maintenance. In a task of CGUI package of LIUtils,
I found that bad localization loader doesn't support the blank placeholders(half space). We lost a way to make the localized fields ordered. But this make using better specification more important.
在该Localization作业中，我使用的格式规范如下。
In this task of Localization, the specification I used are as follows:

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

CGUILang looks like this:
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

This effect is very possible. You can refer to this designing method.
One more thing, please use the same name for simular fields when you call the fields to make the localization file as simple as possible.

Auther: KS
Translator: GISDYT
