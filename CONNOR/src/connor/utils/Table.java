package connor.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;

import java.util.*;

/**
 * Class used to efficiently handle tables with fixed schemas
 *
 * @author hayats
 */
public class Table implements Jsonable {

	private boolean isInitTable;

	private final List<Var> schema;
	private final Set<List<Node>> rows;

	/**
	 * Used to create an initialisation table of a given arity
	 *
	 * @param arity arity of the initialisation table
	 * @return an empty table with properly named vars
	 */
	public static Table ofArity(int arity) {
		List<Var> vars = new ArrayList<>();
		for(int i = 0; i < arity; i++) {
			vars.add(Var.alloc(String.format("Neighbor_%d", i)));
		}

		Table t = new Table(vars);
		t.isInitTable = true;
		return t;
	}

	/**
	 * Base constructor to create a table with a given schema.
	 *
	 * @param vars the list of {@link Var vars} forming the schema of the table
	 */
	public Table(List<Var> vars) {
		Set<Var> varSet = new HashSet<>(vars);
		if(varSet.size() != vars.size()) {
			throw new TableException("Trying to create a table with redundant Vars in the schema schema");
		}
		this.schema = new ArrayList<>(vars);
		this.rows = new HashSet<>();
		this.isInitTable = false;
	}

	/**
	 * @return an {@link Iterator} on the rows of the table
	 */
	public Iterator<List<Node>> rows() {
		return this.rows.iterator();
	}

	/**
	 * @return the schema of the table
	 */
	public List<Var> getSchema() {
		return schema;
	}

	/**
	 * @return the number of rows of the table
	 */
	public int size() {
		return rows.size();
	}

	/**
	 * @return true if the table has no row, false otherwise
	 */
	public boolean isEmpty() {
		return rows.size() == 0;
	}

	/**
	 * Add a row to the table. Works only if it is an initialization table.
	 *
	 * @param row the list of {@link Node nodes} representing the
	 */
	public void addInitRow(List<Node> row) {
		if(!this.isInitTable) {
			throw new TableException("This method can only be applied on initialization tables");
		}
		this.addRow(row);
	}

	/**
	 * Add a row represented by a {@link Binding} to the table
	 *
	 * @param binding the binding representing the row to add
	 */
	public void addBinding(Binding binding) {
		List<Node> row = new ArrayList<>();

		for(Var v : this.schema) {
			row.add(binding.get(v));
		}
		rows.add(row);
	}

	/**
	 * Compute the projection of the table on a list of variables
	 *
	 * @param projectionVars the list of {@link Var vars} on which to project
	 * @return the projected table
	 */
	public Table projection(List<Var> projectionVars) {
		//System.out.println("the variables: " + projectionVars);
		//System.out.println("the schema: " + this.schema);
		if(!this.schema.containsAll(projectionVars)) {
			for (Var var : projectionVars) {
				if (!this.schema.contains(var)) {
					System.out.println(" - " + var);
				}
			}
			throw new TableException("Tried to project a table on a non-existing variable");
		}

		if(projectionVars.containsAll(this.schema)) {
			return this;
		}

		Table projectedTable = new Table(projectionVars);

		Map<Var, Integer> varToIndex = new HashMap<>();
		projectionVars.forEach(var -> varToIndex.put(var, this.schema.indexOf(var)));

		for(List<Node> row : this.rows) {
			List<Node> projectedRow = new ArrayList<>();
			for(Var v : projectionVars) {
				projectedRow.add(row.get(varToIndex.get(v)));
			}
			projectedTable.addRow(projectedRow);
		}

		return projectedTable;
	}

	/**
	 * Compute the intersection of the table with another table of same schema
	 *
	 * @param table the table with which we want to intersect
	 * @return a table of same schema representing the intersection of the tables
	 */
	public Table intersect(Table table) {
		if(!((this.schema.containsAll(table.schema)) && (table.schema.containsAll(this.schema)))) {
			throw new TableException("Tried to intersect two tables of different schemas");
		}

		Map<Integer, Integer> thisToTable = new HashMap<>();
		Map<Integer, Integer> tableToThis = new HashMap<>();
		for(int i = 0; i < this.schema.size(); i++) {
			thisToTable.put(i, table.schema.indexOf(this.schema.get(i)));
			tableToThis.put(i, this.schema.indexOf(table.schema.get(i)));
		}

		Table intersect = new Table(this.schema);
		if(this.size() < table.size()) {
			for(List<Node> row : this.rows) {
				List<Node> recreatedRow = new ArrayList<>();
				for(int i = 0; i < this.schema.size(); i++) {
					recreatedRow.add(row.get(tableToThis.get(i)));
				}
				if(table.rows.contains(recreatedRow)) {
					intersect.addRow(row);
				}
			}
		} else {
			for(List<Node> row : table.rows) {
				List<Node> recreatedRow = new ArrayList<>();
				for(int i = 0; i < this.schema.size(); i++) {
					recreatedRow.add(row.get(thisToTable.get(i)));
				}
				if(this.rows.contains(recreatedRow)) {
					intersect.addRow(row);
				}
			}
		}

		return intersect;
	}

	/**
	 * Compute the difference of this table with another table of same schema
	 * @param table the table we want to have the difference with
	 * @return a table of same schema representing the difference of the tables
	 */
	public Table difference(Table table) {
		if(!(this.schema.containsAll(table.schema)) && (table.schema.containsAll(this.schema))) {
			throw new TableException("Tried to compute the difference two tables of different schemas");
		}

		Map<Integer, Integer> tableToThis = new HashMap<>();
		for(int i = 0; i < this.schema.size(); i++) {
			tableToThis.put(i, this.schema.indexOf(table.schema.get(i)));
		}

		Table difference = new Table(this.schema);
		for(List<Node> row : this.rows) {
			List<Node> recreatedRow = new ArrayList<>();
			for(int i = 0; i < this.schema.size(); i++) {
				recreatedRow.add(row.get(tableToThis.get(i)));
			}
			if(!table.rows.contains(recreatedRow)) {
				difference.addRow(row);
			}
		}

		return difference;
	}

	/**
	 * Compute the join of this table with another table
	 * @param table2 the table with which we want to join
	 * @return a table representing the join
	 */
	public Table join(Table table2) {
		if(this.schema.containsAll(table2.schema) && table2.schema.containsAll(this.schema)) {
			// Table of same schemas -> intersection
			return this.intersect(table2);
		} else if(table2.schema.containsAll(this.schema)) {
			// A tables schema is included in the other tables
			Table join = new Table(table2.schema);

			Map<Integer, Integer> correspondingTable = new HashMap<>();
			for(int i = 0; i < this.schema.size(); i++) {
				correspondingTable.put(i, table2.schema.indexOf(this.schema.get(i)));
			}

			for(List<Node> fullRow : table2.rows) {
				List<Node> recreatedRow = new ArrayList<>();
				for(int i = 0; i < this.schema.size(); i++) {
					recreatedRow.add(fullRow.get(correspondingTable.get(i)));
				}
				if(this.rows.contains(recreatedRow)) {
					join.addRow(fullRow);
				}
			}
			return join;
		} else if(this.schema.containsAll(table2.schema)) {
			// Same as previous but inverted
			return table2.join(this);
		} else {
			// General case
			List<Var> table1Exclusive = new ArrayList<>();
			List<Var> intersectionScheme = new ArrayList<>();
			List<Var> table2Exclusive = new ArrayList<>();

			Table table1 = this;

			for(Var var : table1.schema) {
				if(!table2.schema.contains(var)) {
					table1Exclusive.add(var);
				} else {
					intersectionScheme.add(var);
				}
			}

			for(Var var : table2.schema) {
				if(!table1.schema.contains(var)) {
					table2Exclusive.add(var);
				}
			}

			List<Var> joinSchema = new ArrayList<>();
			joinSchema.addAll(table1Exclusive);
			joinSchema.addAll(intersectionScheme);
			joinSchema.addAll(table2Exclusive);

			Map<Integer, Integer> exclusiveToTable1 = new HashMap<>();
			Map<Integer, Integer> intersectToTable1 = new HashMap<>();
			Map<Integer, Integer> intersectToTable2 = new HashMap<>();
			Map<Integer, Integer> exclusiveToTable2 = new HashMap<>();

			for(int i = 0; i < table1Exclusive.size(); i++) {
				exclusiveToTable1.put(i, table1.schema.indexOf(table1Exclusive.get(i)));
			}
			for(int i = 0; i < intersectionScheme.size(); i++) {
				intersectToTable1.put(i, table1.schema.indexOf(intersectionScheme.get(i)));
				intersectToTable2.put(i, table2.schema.indexOf(intersectionScheme.get(i)));
			}
			for(int i = 0; i < table2Exclusive.size(); i++) {
				exclusiveToTable2.put(i, table2.schema.indexOf(table2Exclusive.get(i)));
			}

			Table join = new Table(joinSchema);

			for(List<Node> row1 : table1.rows) {
				for(List<Node> row2 : table2.rows) {
					boolean compatible = true;
					int i = 0;
					while(compatible && i < intersectionScheme.size()) {
						if(!row1.get(intersectToTable1.get(i)).equals(row2.get(intersectToTable2.get(i)))) {
							compatible = false;
						}
						i++;
					}
					if(compatible) {
						List<Node> row = new ArrayList<>();
						for(int j = 0; j < table1Exclusive.size(); j++) {
							row.add(row1.get(exclusiveToTable1.get(j)));
						}
						for(int j = 0; j < intersectionScheme.size(); j++) {
							row.add(row1.get(intersectToTable1.get(j)));
						}
						for(int j = 0; j < table2Exclusive.size(); j++) {
							row.add(row2.get(exclusiveToTable2.get(j)));
						}
						join.addRow(row);
					}
				}
			}
			return join;
		}
	}

	/**
	 * Serialise the table into a JSON object
	 * @return a string representing this JSON object
	 */
	@Override
	public String toJson() {
		StringBuilder json = new StringBuilder("[\n");
		for(Iterator<List<Node>> rowsIter = this.rows.iterator(); rowsIter.hasNext(); ) {
			List<Node> row = rowsIter.next();

			StringBuilder line = new StringBuilder("\t[");

			for(int i = 0; i < row.size(); i++) {
				line.append("\"").append(row.get(i).toString()).append("\"");
				if(i < row.size() - 1) {
					line.append(",");
				}
			}
			line.append("]");
			if(rowsIter.hasNext()) {
				line.append(",");
			}
			line.append("\n");

			json.append(line);

		}
		json.append("]");
		return json.toString();
	}

	private void addRow(List<Node> row) throws TableException {
		if(row.size() != this.schema.size()) {
			throw new TableException("Trying to add a malformed row");
		}
		this.rows.add(row);
	}
}
