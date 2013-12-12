/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.android.internal.view.menu.ExpandedMenuView;
import com.android.internal.view.menu.IconMenuItemView;
import com.android.internal.view.menu.IconMenuView;
import com.android.internal.app.AlertController.RecycleListView;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;

/**
 * This class is used to instantiate layout XML file into its corresponding View
 * objects. It is never be used directly -- use
 * {@link android.app.Activity#getLayoutInflater()} or
 * {@link Context#getSystemService} to retrieve a standard LayoutInflater
 * instance that is already hooked up to the current context and correctly
 * configured for the device you are running on. For example:
 * 
 * <pre>
 * LayoutInflater inflater = (LayoutInflater)context.getSystemService
 *      Context.LAYOUT_INFLATER_SERVICE);
 * </pre>
 * 
 * <p>
 * To create a new LayoutInflater with an additional {@link Factory} for your
 * own views, you can use {@link #cloneInContext} to clone an existing
 * ViewFactory, and then call {@link #setFactory} on it to include your Factory.
 * 
 * <p>
 * For performance reasons, view inflation relies heavily on pre-processing of
 * XML files that is done at build time. Therefore, it is not currently possible
 * to use LayoutInflater with an XmlPullParser over a plain XML file at runtime;
 * it only works with an XmlPullParser returned from a compiled resource (R.
 * <em>something</em> file.)
 * 
 * @see Context#getSystemService
 */
public class LayoutInflater {
    private final HashMap<String, Class<?>> mLoadedClasses = new HashMap<String, Class<?>>();
	private static final String[] sClassPrefixList = { "android.widget.",
			"android.webkit.", "android.view." };

	protected final Context mContext;
	private static final String TAG = "LayoutInflater";
    private static final String TAG_MERGE = "merge";

	// these are optional, set by the caller
	private boolean mFactorySet;
	private Factory mFactory;
	private Filter mFilter;

	private final Object[] mConstructorArgs = new Object[2];

	private static final Class[] mConstructorSignature = new Class[] {
			Context.class, AttributeSet.class };

	private static final HashMap<String, Constructor> sConstructorMap = new HashMap<String, Constructor>();

	private HashMap<String, Boolean> mFilterMap;
    private static final String TAG_REQUEST_FOCUS = "requestFocus";

	/**
	 * Hook to allow clients of the LayoutInflater to restrict the set of Views
	 * that are allowed to be inflated.
	 * 
	 */
	public interface Filter {
		/**
		 * Hook to allow clients of the LayoutInflater to restrict the set of
		 * Views that are allowed to be inflated.
		 * 
		 * @param clazz
		 *            The class object for the View that is about to be inflated
		 * 
		 * @return True if this class is allowed to be inflated, or false
		 *         otherwise
		 */
		boolean onLoadClass(Class clazz);
	}

	public interface Factory {
		/**
		 * Hook you can supply that is called when inflating from a
		 * LayoutInflater. You can use this to customize the tag names available
		 * in your XML layout files.
		 * 
		 * <p>
		 * Note that it is good practice to prefix these custom names with your
		 * package (i.e., com.coolcompany.apps) to avoid conflicts with system
		 * names.
		 * 
		 * @param name
		 *            Tag name to be inflated.
		 * @param context
		 *            The context the view is being created in.
		 * @param attrs
		 *            Inflation attributes as specified in XML file.
		 * 
		 * @return View Newly created view. Return null for the default
		 *         behavior.
		 */
		public View onCreateView(String name, Context context,
				AttributeSet attrs);
	}

	private static class FactoryMerger implements Factory {
		private final Factory mF1, mF2;

		FactoryMerger(Factory f1, Factory f2) {
			mF1 = f1;
			mF2 = f2;
		}

		public View onCreateView(String name, Context context,
				AttributeSet attrs) {
			View v = mF1.onCreateView(name, context, attrs);
			if (v != null)
				return v;
			return mF2.onCreateView(name, context, attrs);
		}
	}

	/**
	 * Create a new LayoutInflater instance associated with a particular
	 * Context. Applications will almost always want to use
	 * {@link Context#getSystemService Context.getSystemService()} to retrieve
	 * the standard {@link Context#LAYOUT_INFLATER_SERVICE
	 * Context.INFLATER_SERVICE}.
	 * 
	 * @param context
	 *            The Context in which this LayoutInflater will create its
	 *            Views; most importantly, this supplies the theme from which
	 *            the default values for their attributes are retrieved.
	 */
	public LayoutInflater(Context context) {
		mContext = context;
	}

	/**
	 * Create a new LayoutInflater instance that is a copy of an existing
	 * LayoutInflater, optionally with its Context changed. For use in
	 * implementing {@link #cloneInContext}.
	 * 
	 * @param original
	 *            The original LayoutInflater to copy.
	 * @param newContext
	 *            The new Context to use.
	 */
	protected LayoutInflater(LayoutInflater original, Context newContext) {
		mContext = newContext;
		mFactory = original.mFactory;
		mFilter = original.mFilter;
	}

	/**
	 * Obtains the LayoutInflater from the given context.
	 */
	public static LayoutInflater from(Context context) {
		LayoutInflater LayoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (LayoutInflater == null) {
			throw new AssertionError("LayoutInflater not found.");
		}
		return LayoutInflater;
	}

	/**
	 * Create a copy of the existing LayoutInflater object, with the copy
	 * pointing to a different Context than the original. This is used by
	 * {@link ContextThemeWrapper} to create a new LayoutInflater to go along
	 * with the new Context theme.
	 * 
	 * @param newContext
	 *            The new Context to associate with the new LayoutInflater. May
	 *            be the same as the original Context if desired.
	 * 
	 * @return Returns a brand spanking new LayoutInflater object associated
	 *         with the given Context.
	 */
	public LayoutInflater cloneInContext(Context newContext) {
		return new LayoutInflater(this, newContext);
	}

	/**
	 * Return the context we are running in, for access to resources, class
	 * loader, etc.
	 */
	public Context getContext() {
		return mContext;
	}

	/**
	 * Return the current factory (or null). This is called on each element
	 * name. If the factory returns a View, add that to the hierarchy. If it
	 * returns null, proceed to call onCreateView(name).
	 */
	public final Factory getFactory() {
		return mFactory;
	}

	/**
	 * Attach a custom Factory interface for creating views while using this
	 * LayoutInflater. This must not be null, and can only be set once; after
	 * setting, you can not change the factory. This is called on each element
	 * name as the xml is parsed. If the factory returns a View, that is added
	 * to the hierarchy. If it returns null, the next factory default
	 * {@link #onCreateView} method is called.
	 * 
	 * <p>
	 * If you have an existing LayoutInflater and want to add your own factory
	 * to it, use {@link #cloneInContext} to clone the existing instance and
	 * then you can use this function (once) on the returned new instance. This
	 * will merge your own factory with whatever factory the original instance
	 * is using.
	 */
	public void setFactory(Factory factory) {
		if (mFactorySet) {
			throw new IllegalStateException(
					"A factory has already been set on this LayoutInflater");
		}
		if (factory == null) {
			throw new NullPointerException("Given factory can not be null");
		}
		mFactorySet = true;
		if (mFactory == null) {
			mFactory = factory;
		} else {
			mFactory = new FactoryMerger(factory, mFactory);
		}
	}

	/**
	 * @return The {@link Filter} currently used by this LayoutInflater to
	 *         restrict the set of Views that are allowed to be inflated.
	 */
	public Filter getFilter() {
		return mFilter;
	}

	/**
	 * Sets the {@link Filter} to by this LayoutInflater. If a view is attempted
	 * to be inflated which is not allowed by the {@link Filter}, the
	 * {@link #inflate(int, ViewGroup)} call will throw an
	 * {@link InflateException}. This filter will replace any previous filter
	 * set on this LayoutInflater.
	 * 
	 * @param filter
	 *            The Filter which restricts the set of Views that are allowed
	 *            to be inflated. This filter will replace any previous filter
	 *            set on this LayoutInflater.
	 */
	public void setFilter(Filter filter) {
		mFilter = filter;
		if (filter != null) {
			mFilterMap = new HashMap<String, Boolean>();
		}
	}

	/**
	 * Inflate a new view hierarchy from the specified xml resource. Throws
	 * {@link InflateException} if there is an error.
	 * 
	 * @param resource
	 *            ID for an XML layout resource to load (e.g.,
	 *            <code>R.layout.main_page</code>)
	 * @param root
	 *            Optional view to be the parent of the generated hierarchy.
	 * @return The root View of the inflated hierarchy. If root was supplied,
	 *         this is the root View; otherwise it is the root of the inflated
	 *         XML file.
	 */
	public View inflate(int resource, ViewGroup root) {
		return inflate(resource, root, root != null);
	}

	/**
	 * Inflate a new view hierarchy from the specified xml node. Throws
	 * {@link InflateException} if there is an error. *
	 * <p>
	 * <em><strong>Important</strong></em>&nbsp;&nbsp;&nbsp;For performance
	 * reasons, view inflation relies heavily on pre-processing of XML files
	 * that is done at build time. Therefore, it is not currently possible to
	 * use LayoutInflater with an XmlPullParser over a plain XML file at
	 * runtime.
	 * 
	 * @param parser
	 *            XML dom node containing the description of the view hierarchy.
	 * @param root
	 *            Optional view to be the parent of the generated hierarchy.
	 * @return The root View of the inflated hierarchy. If root was supplied,
	 *         this is the root View; otherwise it is the root of the inflated
	 *         XML file.
	 */
	public View inflate(XmlPullParser parser, ViewGroup root) {
		return inflate(parser, root, root != null);
	}

	/**
	 * Inflate a new view hierarchy from the specified xml resource. Throws
	 * {@link InflateException} if there is an error.
	 * 
	 * @param resource
	 *            ID for an XML layout resource to load (e.g.,
	 *            <code>R.layout.main_page</code>)
	 * @param root
	 *            Optional view to be the parent of the generated hierarchy (if
	 *            <em>attachToRoot</em> is true), or else simply an object that
	 *            provides a set of LayoutParams values for root of the returned
	 *            hierarchy (if <em>attachToRoot</em> is false.)
	 * @param attachToRoot
	 *            Whether the inflated hierarchy should be attached to the root
	 *            parameter? If false, root is only used to create the correct
	 *            subclass of LayoutParams for the root view in the XML.
	 * @return The root View of the inflated hierarchy. If root was supplied and
	 *         attachToRoot is true, this is root; otherwise it is the root of
	 *         the inflated XML file.
	 */
	public View inflate(int resource, ViewGroup root, boolean attachToRoot) {
		XmlResourceParser parser = getContext().getResources().getLayout(
				resource);
		try {
			return inflate(parser, root, attachToRoot);
		} finally {
			parser.close();
		}
	}

	/**
	 * Inflate a new view hierarchy from the specified XML node. Throws
	 * {@link InflateException} if there is an error.
	 * <p>
	 * <em><strong>Important</strong></em>&nbsp;&nbsp;&nbsp;For performance
	 * reasons, view inflation relies heavily on pre-processing of XML files
	 * that is done at build time. Therefore, it is not currently possible to
	 * use LayoutInflater with an XmlPullParser over a plain XML file at
	 * runtime.
	 * 
	 * @param parser
	 *            XML dom node containing the description of the view hierarchy.
	 * @param root
	 *            Optional view to be the parent of the generated hierarchy (if
	 *            <em>attachToRoot</em> is true), or else simply an object that
	 *            provides a set of LayoutParams values for root of the returned
	 *            hierarchy (if <em>attachToRoot</em> is false.)
	 * @param attachToRoot
	 *            Whether the inflated hierarchy should be attached to the root
	 *            parameter? If false, root is only used to create the correct
	 *            subclass of LayoutParams for the root view in the XML.
	 * @return The root View of the inflated hierarchy. If root was supplied and
	 *         attachToRoot is true, this is root; otherwise it is the root of
	 *         the inflated XML file.
	 */
	public View inflate(XmlPullParser parser, ViewGroup root,
			boolean attachToRoot) {
		final AttributeSet attrs = (AttributeSet) parser;
		Context lastContext = (Context) mConstructorArgs[0];
		mConstructorArgs[0] = mContext;

        View result = root;
		// Look for the root node.
		int type = -1;
		try {
            if (parser == null) {
                 throw new RuntimeException("LayoutInflater.inflate -> parser is null");
            }
			while ((type = parser.next()) != XmlPullParser.START_TAG
					&& type != XmlPullParser.END_DOCUMENT) {
				// Empty
			}
		

		if (type != XmlPullParser.START_TAG) {
			throw new InflateException(parser.getPositionDescription()
					+ ": No start tag found!");
		}

		final String name = parser.getName();
		//            System.out.println("**************************");
		//            System.out.println("Creating root view: "
		//                    + name);
		//            System.out.println("**************************");

		//		XmlResourceParser.XMLNode rootNode = parser.getRoot();
		// temp is the root view that was found in the xml

        if (TAG_MERGE.equals(name)) {
            if (root == null || !attachToRoot) {
                throw new InflateException("<merge /> can be used only with a valid "
                        + "ViewGroup root and attachToRoot=true");
            }

            rInflate(parser, root, attrs);
        } else {
                View temp = createViewFromTag(name, attrs);
                Log.i(TAG, "inflate>>>createView success");
                ViewGroup.LayoutParams params = null;
                if (root != null) {
                    // Create layout params that match root, if supplied
                    params = root.generateLayoutParams(attrs);

                    if (attachToRoot == false) {
                        // Set the layout params for temp if we are not
                        // attaching. (If we are, we use addView, below)
                        // temp.setLayoutParams(params);
                        // int uiid = temp.getUIElementID();
                        // /**
                        // @j2sNative
                        // var thisView = document.getElementById(uiid);
                        // if (null == thisView) {
                        // thisView = document.createElement("span");
                        // thisView.style.position = "absolute";
                        // thisView.id = uiid;
                        // thisView.style.zIndex = 10000;
                        // thisView.style.display = "block";
                        // thisView.style.visibility = "hidden";
                        // }
                        // document.body.appendChild(thisView);
                        // */
                        // {
                        // }
                        // System.out.println("root ID: " +
                        // root.getUIElementID());
                        root.addView(temp, params, false);
                    } else {
                        // System.out.println("root ID: " +
                        // root.getUIElementID());
                        root.addView(temp, params);
                    }
                }
                // Inflate all children under temp
                rInflate(parser, temp, attrs);
                // Decide whether to return the root that was passed in or the
                // top view found in xml.
                if (root == null || attachToRoot == false) {
                    result = temp;
                }
        }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        // Don't retain static reference on context.
            mConstructorArgs[0] = lastContext;
            mConstructorArgs[1] = null;
        }

		return result;
	}

	/**
	 * Low-level function for instantiating a view by name. This attempts to
	 * instantiate a view class of the given <var>name</var> found in this
	 * LayoutInflater's ClassLoader.
	 * 
	 * <p>
	 * There are two things that can happen in an error case: either the
	 * exception describing the error will be thrown, or a null will be
	 * returned. You must deal with both possibilities -- the former will happen
	 * the first time createView() is called for a class of a particular name,
	 * the latter every time there-after for that class name.
	 * 
	 * @param name
	 *            The full name of the class to be instantiated.
	 * @param attrs
	 *            The XML attributes supplied for this instance.
	 * 
	 * @return View The newly instantied view, or null.
	 */
	public final View createView(String name, String prefix, AttributeSet attrs)
			throws ClassNotFoundException, InflateException {
		Log.i(TAG, "createView:"+name+":"+prefix);
		Constructor constructor = sConstructorMap.get(name);
		Class clazz = null;

		try {
			if (constructor == null) {
				// Class not found in the cache, see if it's real, and try to
				// add it
				
				String classPath = prefix != null ? (prefix + name) : name;
				if (null == clazz) {
					clazz = Class.forName(classPath);
				}

                if (clazz == null) {
                    Log.w(TAG, "createView:" + clazz);
                    throw new ClassNotFoundException("LayoutInflater.createView:<" + classPath
                            + "> class not found");
                }

				if (mFilter != null && clazz != null) {
					Log.i(TAG, "mFilter is not null");
					boolean allowed = mFilter.onLoadClass(clazz);
					
					if (!allowed) {
						failNotAllowed(name, prefix, attrs);
					}
				}

				constructor = clazz.getConstructor(mConstructorSignature);
				//				Class[] clazzes = constructor.getParameterTypes();
				//				for (int i = 0; i < clazzes.length; ++i) {
				//					System.out.println("param #" + i + ": "
				//							+ clazzes[i].getName());
				//				}
				if (null == constructor) {
					Log.i(TAG, "constructor is null");
				}
				sConstructorMap.put(name, constructor);
			} else {
				// If we have a filter, apply it to cached constructor
				if (mFilter != null) {
					// Have we seen this name before?
					Boolean allowedState = mFilterMap.get(name);
					if (allowedState == null) {
//						// New class -- remember whether it is allowed
//						clazz = mContext.getClassLoader().loadClass(prefix != null ? (prefix + name) : name);
//
//						boolean allowed = clazz != null
//								&& mFilter.onLoadClass(clazz);
//						mFilterMap.put(name, allowed);
//						if (!allowed) {
//							failNotAllowed(name, prefix, attrs);
//						}
					} else if (allowedState.equals(Boolean.FALSE)) {
						failNotAllowed(name, prefix, attrs);
					}
				}
			}

			Object[] args = mConstructorArgs;
			args[1] = attrs;
			return (View) constructor.newInstance(args);
		} catch (NoSuchMethodException e) {
			InflateException ie = new InflateException(
					attrs.getPositionDescription() + ": Error inflating class "
							+ (prefix != null ? (prefix + name) : name));
			ie.initCause(e);
			throw ie;

		} catch (ClassNotFoundException e) {
			// If loadClass fails, we should propagate the exception.
			throw e;
		} catch (Exception e) {
			InflateException ie = new InflateException(
					attrs.getPositionDescription() + ": Error inflating class "
							+ (clazz == null ? "<unknown>" : clazz.getName()));
			ie.initCause(e);
			e.printStackTrace();
			throw ie;
		}
	}

	/**
	 * Throw an excpetion because the specified class is not allowed to be
	 * inflated.
	 */
	private void failNotAllowed(String name, String prefix, AttributeSet attrs) {
		InflateException ie = new InflateException(
				attrs.getPositionDescription()
						+ ": Class not allowed to be inflated "
						+ (prefix != null ? (prefix + name) : name));
		throw ie;
	}

	/**
	 * This routine is responsible for creating the correct subclass of View
	 * given the xml element name. Override it to handle custom view objects. If
	 * you override this in your subclass be sure to call through to
	 * super.onCreateView(name) for names you do not recognize.
	 * 
	 * @param name
	 *            The fully qualified class name of the View to be create.
	 * @param attrs
	 *            An AttributeSet of attributes to apply to the View.
	 * 
	 * @return View The View created.
	 */
    public View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        View view = null;
        for (String prefix : sClassPrefixList) {
            try {
                String classPath = prefix + name;
                Class clazz = Class.forName(classPath);
                if(clazz != null) {
                    view = createView(name, prefix, attrs);
                }
            } catch (ClassNotFoundException e) {
            }
            if (view != null) {
                break;
            }
        }

        return view;
    }

	/**
	 * default visibility so the BridgeInflater can override it.
	 */
    @SuppressWarnings("finally")
	View createViewFromTag(String name, AttributeSet attrs) {
//		System.out.println("******** Creating view: " + name);
        View view = null;
        if (name.equals("view")) {
            name = attrs.getAttributeValue(null, "class");
        }
		try {
			view = (mFactory == null) ? null : mFactory.onCreateView(name,
					mContext, attrs);
//			System.out.println("CreateView:"+(view==null?"null":view));
			if (view == null) {
				if (-1 == name.indexOf('.')) {
					view = onCreateView(name, attrs);
				} else {
					view = createView(name, null, attrs);
				}
			}
            Log.i(TAG, "******** Creating view: " + name
					+ " Done! UIID: " + view.getUIElementID() + ", ID: "
					+ view.getId());
						if (view.getUIElementID() == 47)
							System.out.println();
        } catch (Exception e) {
            InflateException ie = new InflateException(
                    attrs.getPositionDescription() + ": Error inflating class "
                            + name);
            ie.initCause(e);
            throw ie;
        } finally {
            return view;
        }
        
    }

	/**
	 * Recursive method used to descend down the xml hierarchy and instantiate
	 * views, instantiate their children, and then call onFinishInflate().
	 */
	private void rInflate(XmlPullParser parser, View parent, AttributeSet attrs) {
        Log.i(TAG, "in rInflate, for View: " + parent.getUIElementID());
		final int depth = parser.getDepth();
		int type;

		try {
			while (((type = parser.next()) != XmlPullParser.END_TAG || parser
					.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

				if (type != XmlPullParser.START_TAG) {
					continue;
				}
				final String name = parser.getName();

                if (TAG_REQUEST_FOCUS.equals(name)) {
                    parseRequestFocus(parser, parent);
                } else {
                    final View view = createViewFromTag(name, attrs);
                    final ViewGroup viewGroup = (ViewGroup) parent;
                    final ViewGroup.LayoutParams params = viewGroup
                            .generateLayoutParams(attrs);
                    viewGroup.addView(view, params);
                    rInflate(parser, view, attrs);
                }
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		parent.onFinishInflate();
//		System.out.println("rInflate done, for View: "
//				+ parent.getUIElementID());
	}

    private void parseRequestFocus(XmlPullParser parser, View parent)
            throws XmlPullParserException, IOException {
        int type;
        parent.requestFocus();
        final int currentDepth = parser.getDepth();
        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > currentDepth) && type != XmlPullParser.END_DOCUMENT) {
            // Empty
        }
    }
}
