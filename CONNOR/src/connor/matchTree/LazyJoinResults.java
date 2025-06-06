package connor.matchTree;

import org.apache.jena.sparql.core.Var;

import java.util.Set;

/**
 * Class used to stock the result of the lazy join operation.
 * Should not be used by end users.
 *
 * @author hayats
 */
public class LazyJoinResults {
	private final boolean isModified;
	private final Set<Var> propagationVarsMinus;
	private final Set<Var> propagationVarsPlus;
	private final MatchTree matchTree;

	public LazyJoinResults(boolean isModified, Set<Var> propagationVarsMinus, Set<Var> propagationVarsPlus, MatchTree matchTree) {
		this.isModified = isModified;
		this.propagationVarsMinus = propagationVarsMinus;
		this.propagationVarsPlus = propagationVarsPlus;
		this.matchTree = (isModified?matchTree:null);
	}

	public boolean isModified() {
		return isModified;
	}

	public Set<Var> getPropagationVarsMinus() {
		return propagationVarsMinus;
	}

	public Set<Var> getPropagationVarsPlus() {
		return propagationVarsPlus;
	}

	public MatchTree getMatchTree() {
		return matchTree;
	}
}
