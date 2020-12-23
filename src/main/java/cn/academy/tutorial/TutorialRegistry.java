package cn.academy.tutorial;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class handles tutorials' general registry.
 */
public class TutorialRegistry {

    private static HashMap<String,ACTutorial> tutorials=new LinkedHashMap<>();

    public static void addTutorials(ACTutorial...tutorial) {
        for(ACTutorial t : tutorial){
            if(tutorials.containsKey(t.id))
                throw new RuntimeException("Already have a tutorial with this id:" + t.id);
            tutorials.put(t.id, t);
        }
    }

    public static ACTutorial addTutorial(String string) {
        ACTutorial t=new ACTutorial(string);
        addTutorials(t);
        return t;
    }

    public static void addTutorials(String...string) {
        ACTutorial[] acTu=new ACTutorial[string.length];
        int i = 0;
        for(String s : string){
            acTu[i] = new ACTutorial(s);
            i++;
        }
        addTutorials(acTu);
    }

    public static ACTutorial getTutorial(String s) {
        ACTutorial t;
        if(!tutorials.containsKey(s))
            throw new RuntimeException("No such a tutorial;");
        t=tutorials.get(s);
        return t;
    }

    /**
     * Get a collection of tutorial learned by the player.
     */
    public static Collection<ACTutorial> getLearned(EntityPlayer player) {
        return tutorials
                .values()
                .stream()
                .filter(t -> t.isActivated(player))
                .collect(Collectors.toList());
    }

    /**
     * Get two list of tutorials, one is the set of learned, another the set of unlearned.
     */
    public static Pair<List<ACTutorial>, List<ACTutorial>> groupByLearned(EntityPlayer player) {
        List<ACTutorial> learned = new ArrayList<>();
        List<ACTutorial> unlearned = new ArrayList<>();

        for (ACTutorial tut : tutorials.values()) {
            if (tut.isActivated(player)) {
                learned.add(tut);
            } else {
                unlearned.add(tut);
            }
        }

        return Pair.of(learned, unlearned);
    }

    /**
     * Get a immutable enumeration of all registered tutorial.
     */
    public static Collection<ACTutorial> enumeration() {
        return ImmutableList.copyOf(tutorials.values());
    }

}