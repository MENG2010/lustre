package enums;

// not sure whether we need this
public enum ObservedTreeType {
	REFERECE, SEQUENCE;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
