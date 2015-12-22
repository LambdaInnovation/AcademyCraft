Tutorials
=====

English
=====
This is the source folder of all tutorial texts in AcademyCraft. They are source files for .lang files, and will get compiled into it before release for in-game rendering.

## Rule

Each folder under this folder represents a single lang and maps directly to `<folder_name>.lang`. In the folders contain a series of `.txt` file, all of which represents a single tutorial. The file name is the tutorial's translation key.

A tutorial text's representation is like:

```
![title]

...Title of this tutorial

![beief]

...The brief description of this tutorial

![content]

...The content, or the detailed description of this tutorial
```

If content is not valid (some of the tags not found), the tutorial will be ignored.

## Compilation

To compile all the tutorial contents into .lang file, you can use the simple python script `rebuild_lang.py` provided in this folder. Its usage as follows:

```
python rebuild_lang.py <lang_name>
```

It will loop through all the .txt files in the lang folder and update the content to the corresponding .lang file.

Currently the script depends on the C++ lib md2lang (Whose source is given), and a windows DLL compilation is given. Support for other OS is welcomed.


中文说明
=====

这里是AcademyCraft所有教程文案的源文件夹。它们相当于.lang文件的源文件，在版本发布之前会被编译到.lang文件中，以备运行时渲染。

## 规则

在本文件夹内的所有文件夹都代表一个语言，并且直接对应于 `<文件夹名>.lang`。在每一个文件夹中存放了一系列的`txt`文件，每个都代表一个教程项。文件名是教程项的id名。

一个教程的文字表示规范如下：

```
![title]

...教程标题

![beief]

...教程的简要描述

![content]

...教程的具体内容
```

如果内容不合法（没有找到对应的标签），将会在编译时忽略该教程项。

## 编译

要将教程内容编译到.lang，你可以简单使用本目录下提供的 `rebuild_lang.py` python脚本。它的用法如下：

```
python rebuild_lang.py <语言>
```

它会遍历所有该语言文件夹下的.txt文件，并且将它们的内容更新到对应的.lang文件。

当前，该脚本依赖于C++库md2lang（源代码在本目录下给出），我们默认提供了Windows下的DLL编译。欢迎提供对其他OS的支持> <
