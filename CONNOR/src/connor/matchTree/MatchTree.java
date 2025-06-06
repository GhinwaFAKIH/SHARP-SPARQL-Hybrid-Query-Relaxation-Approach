package connor.matchTree;

import connor.ConnorPartition;
import connor.utils.ElementUtils;
import connor.utils.Table;
import connor.utils.TableException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class used to represent a match-tree (a factorization of a match table) for the computation of concepts of neighbours.
 * Should not be used by end users.
 *
 * @author hayats
 * @author nk-fouque
 */
public class MatchTree {
	private final Element element;
	private final Set<Var> eltVars;

	private final List<Var> newVars;
	private Table matchTable;
	private final List<Var> propagationVars;

	private final Set<MatchTree> children;

	private boolean isInserted;

	/**
	 * Creation of the match-tree of the initial concept
	 *
	 * @param projectionVars projection variable of the initial concept
	 * @param initTable      initial match table
	 */
	public MatchTree(List<Var> projectionVars, Table initTable) {
		this.element = null;
		this.eltVars = new HashSet<>();

		this.newVars = projectionVars;
		this.matchTable = initTable;
		this.propagationVars = projectionVars;

		this.isInserted = true;
		this.children = new HashSet<>();
	}

	/**
	 * Creation of a new node from an element
	 *
	 * @param elt           the {@link org.apache.jena.sparql.syntax.Element element} described by the node
	 * @param partition     the  {@link ConnorPartition} from corresponding to the match-tree
	 * @param varsInConcept the set of {@link org.apache.jena.sparql.core.Var vars} already present in the previous concept
	 */
	public MatchTree(Element elt, ConnorPartition partition, Set<Var> varsInConcept) throws TableException {
		this.element = elt;
		this.eltVars = ElementUtils.mentioned(elt);

		this.newVars = new ArrayList<>(this.eltVars);
		this.newVars.removeAll(varsInConcept);

		this.matchTable = ElementUtils.answer(elt, partition);

		this.propagationVars = new ArrayList<>(this.eltVars);
		this.propagationVars.retainAll(varsInConcept);

		this.isInserted = false;
		this.children = new HashSet<>();
	}

	/**
	 * Deep non-recursive copy of a match-tree node
	 *
	 * @param matchTree the match-tree to copy
	 */
	public MatchTree(MatchTree matchTree) {
		this.element = matchTree.element;
		if(this.element != null) {
			this.eltVars = ElementUtils.mentioned(this.element);
		} else {
			this.eltVars = new HashSet<>();
		}

		this.newVars = matchTree.newVars;
		this.matchTable = matchTree.matchTable;

		this.propagationVars = new ArrayList<>(matchTree.propagationVars);

		this.isInserted = matchTree.isInserted;

		this.children = new HashSet<>();
		this.children.addAll(matchTree.children);
	}

	/**
	 * Getter for the table of the current node
	 *
	 * @return the table of the current node
	 */
	public Table getMatchSet() {
		return this.matchTable;
	}

	/**
	 * 	Implementation of the LazyJoin algorithm
	 *
	 * @param newMatchTree the new match-tree node to insert
	 * @return A {@link LazyJoinResults} object containing the delta-plus, delta-minus and if the node has been modified
	 */
	public LazyJoinResults lazyJoin(MatchTree newMatchTree) throws TableException {
		Set<Var> propagationVarsMinus = new HashSet<>();
		Set<Var> propagationVarsPlus = new HashSet<>();
		boolean isModified = false;
		MatchTree copy = null;

		for(MatchTree child : this.children) {
			LazyJoinResults childResult = child.lazyJoin(newMatchTree);
			propagationVarsMinus.addAll(childResult.getPropagationVarsMinus());
			propagationVarsPlus.addAll(childResult.getPropagationVarsPlus());
			if(childResult.isModified()) {
				isModified = true;
				copy = new MatchTree(this);
				copy.children.remove(child);
				copy.children.add(childResult.getMatchTree());
				child = childResult.getMatchTree();

				copy.matchTable = this.matchTable.join(child.matchTable.projection(child.propagationVars));
			}
		}

		Set<Var> variablesDefinedInNode = new HashSet<>(this.newVars);
		variablesDefinedInNode.retainAll(newMatchTree.propagationVars);
		if(!variablesDefinedInNode.isEmpty()) {
			isModified = true;
			if(copy == null) {
				copy = new MatchTree(this);
			}
			if(!newMatchTree.isInserted) {
				Set<Var> propagationMinusToAdd = new HashSet<>(newMatchTree.propagationVars);
				propagationMinusToAdd.removeAll(this.newVars);
				propagationVarsMinus.addAll(propagationMinusToAdd);
				if (newMatchTree.matchTable != null) {
				copy.matchTable = this.matchTable.join(newMatchTree.matchTable.projection(newMatchTree.propagationVars));}
				copy.children.add(newMatchTree);
				newMatchTree.isInserted = true;
			} else {
				propagationVarsPlus.addAll(variablesDefinedInNode);
			}
		}
		propagationVarsPlus.removeAll(propagationVarsMinus);
		propagationVarsMinus.removeAll(propagationVarsPlus);

		if(isModified) {
			for(Var var : propagationVarsMinus) {
				if(!copy.propagationVars.contains(var)) {
					copy.propagationVars.add(var);
				}
			}
			for(Var var : propagationVarsPlus) {
				if(!copy.propagationVars.contains(var)) {
					copy.propagationVars.add(var);
				}
			}
		} else {
			this.propagationVars.addAll(propagationVarsMinus);
			this.propagationVars.addAll(propagationVarsPlus);
			for(Var var : propagationVarsMinus) {
				if(!this.propagationVars.contains(var)) {
					this.propagationVars.add(var);
				}
			}
			for(Var var : propagationVarsPlus) {
				if(!this.propagationVars.contains(var)) {
					this.propagationVars.add(var);
				}
			}
		}

		return new LazyJoinResults(isModified, propagationVarsMinus, propagationVarsPlus, copy);
	}

}