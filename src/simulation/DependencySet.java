package simulation;

import java.util.HashSet;
import java.util.Set;

import jkind.lustre.Equation;

/**
 * The set of variables that an equation depends on
 */
public final class DependencySet implements Comparable<DependencySet> {
	public final Equation equation;
	public final Set<String> dependOn;

	public DependencySet(Equation equation) {
		this.equation = equation;
		this.dependOn = new HashSet<String>();
	}

	@Override
	public int compareTo(DependencySet other) {
		if (this.dependOn.size() > other.dependOn.size()) {
			return 1;
		} else if (this.dependOn.size() == other.dependOn.size()) {
			return 0;
		} else {
			return -1;
		}
	}
}
