package su.hoffmann.foundry.kv;

import java.util.List;

public record KvNode(String key, String value, List<KvNode> children) {

	public boolean isBlock() {
		return children != null;
	}

	public String getString(final String searchKey, final String defaultValue) {
		if (children == null) {
			return defaultValue;
		}
		for (final KvNode child : children) {
			if (child.key().equalsIgnoreCase(searchKey) && !child.isBlock()) {
				return child.value();
			}
		}
		return defaultValue;
	}

	public boolean getBool(final String searchKey, final boolean defaultValue) {
		String value = getString(searchKey, null);
		if (value == null) {
			return defaultValue;
		}
		value = value.trim();
		return value.equals("1") || value.equalsIgnoreCase("true");
	}

	public float getFloat(final String searchKey, final float defaultValue) {
		final String val = getString(searchKey, null);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Float.parseFloat(val.trim());
		} catch (final RuntimeException e) {
			return defaultValue;
		}
	}

	public KvNode getBlock(final String searchKey) {
		if (children == null) {
			return null;
		}
		for (final KvNode child : children) {
			if (child.key().equalsIgnoreCase(searchKey) && child.isBlock()) {
				return child;
			}
		}
		return null;
	}

	public List<KvNode> getAll(final String searchKey) {
		if (children == null) {
			return List.of();
		}
		final java.util.ArrayList<KvNode> result = new java.util.ArrayList<>();
		for (final KvNode child : children) {
			if (child.key().equalsIgnoreCase(searchKey)) {
				result.add(child);
			}
		}
		return result;
	}
}