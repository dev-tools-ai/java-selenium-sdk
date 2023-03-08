package ai.devtools.utils;

import com.google.gson.JsonObject;

import java.util.HashMap;

public class CollectionUtils
{
	/**
	 * Builds a new {@code JsonObject} from the list of Objects. Pass in values such that {@code [ k1, v1, k2, v2, k3, v3... ]}.
	 * 
	 * @param ol The {@code Object}s to use
	 * @return A {@code JsonObject} derived from the values in {@code ol}
	 */
	public static JsonObject keyValuesToJO(Object... ol)
	{
		JsonObject jo = new JsonObject();

		for (int i = 0; i < ol.length; i += 2)
		{
			String k = (String) ol[i];
			Object v = ol[i + 1];

			if (v instanceof String)
				jo.addProperty(k, (String) v);
			else if (v instanceof Number)
				jo.addProperty(k, (Number) v);
			else if (v instanceof Boolean)
				jo.addProperty(k, (Boolean) v);
			else if (v instanceof Character)
				jo.addProperty(k, (Character) v);
			else if (v instanceof JsonObject)
				jo.add(k, (JsonObject) v);
			else
				throw new IllegalArgumentException(String.format("'%s' is not an acceptable type for JSON!", v));
		}

		return jo;
	}

	/**
	 * Simple Tuple implementation. A Tuple is an immutable two-pair of values. It may consist of any two Objects, which may or may not be in of the same type.
	 * 
	 * @param <K> The type of Object allowed for the first Object in the tuple.
	 * @param <V> The type of Object allowed for the second Object in the tuple.
	 */
	public static class Tuple<K, V>
	{
		/**
		 * The k value of the tuple
		 */
		public final K k;

		/**
		 * The v value of the tuple
		 */
		public final V v;

		/**
		 * Constructor, creates a new Tuple from the specified values.
		 * 
		 * @param k The first entry in the Tuple.
		 * @param v The second entry in the Tuple.
		 */
		public Tuple(K k, V v)
		{
			this.k = k;
			this.v = v;
		}
	}
}
