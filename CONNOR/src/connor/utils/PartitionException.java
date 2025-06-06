package connor.utils;

/**
 * Used to signal an error during the partitioning algorithm
 *
 * @author hayats
 */
public class PartitionException extends Exception {
	public PartitionException(String message) {
		super(message);
	}
}
