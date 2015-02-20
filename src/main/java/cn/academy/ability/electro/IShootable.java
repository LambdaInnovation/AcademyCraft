/**
 * 
 */
package cn.academy.ability.electro;

/**
 * Interface for Entity, making one entity operatable by SkillRailgun.
 * The action involved is preparation and progress judgment.
 * SkillRailgun will extract the entity by entityID stored in EntityPlayer.
 * @author WeathFolD
 */
public interface IShootable {
	
	String ENT_ID_FIELD = "shootableId";
	
	/**
	 * Return if the item is being thrown up.
	 */
	boolean inProgress();
	
	/**
	 * Return the current throwing progress. range(0, 1)
	 */
	double getProgress();
	
	/**
	 * Return if we treat a specific tick progress as successful.
	 */
	boolean isAcceptable(double prog);
	
}
