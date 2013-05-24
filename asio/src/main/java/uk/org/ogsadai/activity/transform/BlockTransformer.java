package uk.org.ogsadai.activity.transform;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Defines an interface for efficient serialization of OGSADAI blocks.
 * 
 * @author Chris Borckholder
 */
public interface BlockTransformer /* extends ObjectSerialiser */{

	/**
	 * Serialize the given {@link Object block} and write it to the given
	 * {@link OutputStream}.
	 * 
	 * @param that
	 *            block to be serialized
	 * @param sink
	 *            stream where the block should be written
	 * @throws IOException
	 *             if serialization or writing of the block fails
	 */
	void writeObject(OutputStream sink, Object block) throws IOException;
}
