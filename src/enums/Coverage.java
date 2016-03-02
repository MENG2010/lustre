package enums;

public enum Coverage {
	MCDC, CONDITION, BRANCH, DECISION,
	// add observable coverage options. Meng
	OMCDC, OCONDITION, OBRANCH, ODECISION;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
