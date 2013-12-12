/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.os;

public interface IBinder {
	/**
	 * The first transaction code available for user commands.
	 */
	int FIRST_CALL_TRANSACTION = 0x00000001;
	/**
	 * The last transaction code available for user commands.
	 */
	int LAST_CALL_TRANSACTION = 0x00ffffff;

	/**
	 * IBinder protocol transaction code: pingBinder().
	 */
	int PING_TRANSACTION = ('_' << 24) | ('P' << 16) | ('N' << 8) | 'G';

	/**
	 * IBinder protocol transaction code: dump internal state.
	 */
	int DUMP_TRANSACTION = ('_' << 24) | ('D' << 16) | ('M' << 8) | 'P';

	/**
	 * IBinder protocol transaction code: interrogate the recipient side of the
	 * transaction for its canonical interface descriptor.
	 */
	int INTERFACE_TRANSACTION = ('_' << 24) | ('N' << 16) | ('T' << 8) | 'F';

	/**
	 * Flag to {@link #transact}: this is a one-way call, meaning that the
	 * caller returns immediately, without waiting for a result from the callee.
	 * Applies only if the caller and callee are in different processes.
	 */
	int FLAG_ONEWAY = 0x00000001;

	/**
	 * Get the canonical name of the interface supported by this binder.
	 */
	// public String getInterfaceDescriptor() throws RemoteException;

	/**
	 * Attempt to retrieve a local implementation of an interface for this
	 * Binder object. If null is returned, you will need to instantiate a proxy
	 * class to marshall calls through the transact() method.
	 */
	public IInterface queryLocalInterface(String descriptor);

	/**
	 * Print the object's state into the given stream.
	 * 
	 * @param fd
	 *            The raw file descriptor that the dump is being sent to.
	 * @param args
	 *            additional arguments to the dump request.
	 */
	// public void dump(FileDescriptor fd, String[] args) throws
	// RemoteException;

	/**
	 * Perform a generic operation with the object.
	 * 
	 * @param code
	 *            The action to perform. This should be a number between
	 *            {@link #FIRST_CALL_TRANSACTION} and
	 *            {@link #LAST_CALL_TRANSACTION}.
	 * @param data
	 *            Marshalled data to send to the target. Most not be null. If
	 *            you are not sending any data, you must create an empty Parcel
	 *            that is given here.
	 * @param reply
	 *            Marshalled data to be received from the target. May be null if
	 *            you are not interested in the return value.
	 * @param flags
	 *            Additional operation flags. Either 0 for a normal RPC, or
	 *            {@link #FLAG_ONEWAY} for a one-way RPC.
	 */
	public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException;
	
	public interface DeathRecipient {
        public void binderDied();
    }

//    public boolean isBinderAlive();
//
//    public String getInterfaceDescriptor();
//
//    public boolean pingBinder();
}
