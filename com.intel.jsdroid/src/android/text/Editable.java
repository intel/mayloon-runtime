package android.text;

public interface Editable extends Spannable {

	/**
	* Factory used by TextView to create new Editables.  You can subclass
	* it to provide something other than SpannableStringBuilder.
	*/
	public static class Factory {
		private static Editable.Factory sInstance = new Editable.Factory();

		/**
		 * Returns the standard Editable Factory.
		 */
		public static Editable.Factory getInstance() {
			return sInstance;
		}

		/**
		 * Returns a new SpannedStringBuilder from the specified
		 * CharSequence.  You can override this to provide
		 * a different kind of Spanned.
		 */
		public Editable newEditable(CharSequence source) {
			SpannableStringBuilder ssb = null;
			try {
				ssb = (SpannableStringBuilder) Class.forName(
						"android.text.SpannableStringBuilder").newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return ssb.append(source);
		}
	}

	/**
	 * Replaces the specified range (<code>st&hellip;en</code>) of text in this
	 * Editable with a copy of the slice <code>start&hellip;end</code> from
	 * <code>source</code>.  The destination slice may be empty, in which case
	 * the operation is an insertion, or the source slice may be empty,
	 * in which case the operation is a deletion.
	 * <p>
	 * Before the change is committed, each filter that was set with
	 * {@link #setFilters} is given the opportunity to modify the
	 * <code>source</code> text.
	 * <p>
	 * If <code>source</code>
	 * is Spanned, the spans from it are preserved into the Editable.
	 * Existing spans within the Editable that entirely cover the replaced
	 * range are retained, but any that were strictly within the range
	 * that was replaced are removed.  As a special case, the cursor
	 * position is preserved even when the entire range where it is
	 * located is replaced.
	 * @return  a reference to this object.
	 */
	public Editable replace(int st, int en, CharSequence source, int start,
			int end);

	/**
	 * Convenience for replace(where, where, text, 0, text.length());
	 * @see #replace(int, int, CharSequence, int, int)
	 */
	public Editable insert(int where, CharSequence text);

	/**
	 * Convenience for replace(st, en, "", 0, 0)
	 * @see #replace(int, int, CharSequence, int, int)
	 */
	public Editable delete(int st, int en);

	/**
	 * Convenience for replace(length(), length(), text, 0, text.length())
	 * @see #replace(int, int, CharSequence, int, int)
	 */
	public Editable append(CharSequence text);

	/**
	 * Convenience for replace(length(), length(), text, start, end)
	 * @see #replace(int, int, CharSequence, int, int)
	 */
	public Editable append(CharSequence text, int start, int end);

	/**
	 * Convenience for append(String.valueOf(text)).
	 * @see #replace(int, int, CharSequence, int, int)
	 */
	public Editable append(char text);

	/**
	 * Convenience for replace(0, length(), "", 0, 0)
	 * @see #replace(int, int, CharSequence, int, int)
	 * Note that this clears the text, not the spans;
	 * use {@link #clearSpans} if you need that.
	 */
	public void clear();

	/**
	 * Removes all spans from the Editable, as if by calling
	 * {@link #removeSpan} on each of them.
	 */
	public void clearSpans();
}
