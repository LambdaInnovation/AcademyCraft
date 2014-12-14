package cn.academy.api.ability;

public class Level {
	
	Category parent;

	public Level(Category cat) {
		parent = cat;
	}
	
	void test() {
		System.out.println("b");
	}

}
