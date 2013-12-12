package android.os;

/**
 * A simple wrapper of android's AsyncTask using jsthread. 
 * Maybe consider to use webworker instread later if possible.
 * @author wenhaoli
 */
public abstract class AsyncTask<Params, Progress, Result> {
    private static final String LOG_TAG = "AsyncTask";

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAXIMUM_POOL_SIZE = 128;
    private static final int KEEP_ALIVE = 1;
    
    
    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;
    private static final int MESSAGE_POST_CANCEL = 0x3;
    
    private static final InternalHandler sHandler = new InternalHandler();
    private Status mStatus = Status.PENDING;
    private Params[] mLocalParams;
    private Result mResult;
    /**
     * @j2sNative
     * var mBgThread = null;
     * var mRunnable = null;
     */{}
    
    
    
    public AsyncTask(){
    }
    /**
     * Returns the current status of this task.
     * @return The current status.
     */
    public final Status getStatus() {
        return mStatus;
    }
    
    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     *
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     *
     * @return A result, defined by the subclass of this task.
     *
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    protected abstract Result doInBackground(Params... params);
    
    /**
     * Runs on the UI thread before {@link #doInBackground}.
     *
     * @see #onPostExecute
     * @see #doInBackground
     */
    protected void onPreExecute() {
    }
    /**
     * Runs on the UI thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}
     * or null if the task was cancelled or an exception occured.
     *
     * @param result The result of the operation computed by {@link #doInBackground}.
     *
     * @see #onPreExecute
     * @see #doInBackground
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onPostExecute(Result result) {
    }
    
    /**
     * Runs on the UI thread after {@link #publishProgress} is invoked.
     * The specified values are the values passed to {@link #publishProgress}.
     *
     * @param values The values indicating progress.
     *
     * @see #publishProgress
     * @see #doInBackground
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onProgressUpdate(Progress... values) {
    }
    
    /**
     * Runs on the UI thread after {@link #cancel(boolean)} is invoked.
     *
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    protected void onCancelled() {
    }
    /**
	 * always return false in MayLoon
     */
    public final boolean isCancelled() {
        return false;
    }
    
    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when <tt>cancel</tt> is called,
     * this task should never run.  If the task has already started,
     * then the <tt>mayInterruptIfRunning</tt> parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this
     *        task should be interrupted; otherwise, in-progress tasks are allowed
     *        to complete.
     *
     * @return <tt>false</tt> if the task could not be cancelled,
     *         typically because it has already completed normally;
     *         <tt>true</tt> otherwise
     *
     * @see #isCancelled()
     * @see #onCancelled()
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
       	/**
    	 * @j2sNative
    	 * try{
    	 *   mBgThread.kill(); 
    	 * }catch(e){
    	 *    return false;
    	 * } 
    	 */{}
         Message message = sHandler.obtainMessage(MESSAGE_POST_CANCEL,
                 new AsyncTaskResult<Result>(AsyncTask.this, (Result[]) null));
         message.sendToTarget();
        return false;
    }
    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     * @return The computed result.
     * @throws InterruptedException If the current thread was interrupted
     *         while waiting.
     *         
     * @warning  Actually in MayLoon, it will never throw any exception and may return null
     */
    public final Result get(){
    	/**
    	 * @j2sNative
    	 * try{
    	 * var res = mBgThread.join();
    	 * return this.mResult;
    	 * }catch(e){
    	 *   return null;
    	 * }
    	 */{}
        return null;
    }
    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result.
     *
     * @param timeout Time to wait before cancelling the operation.
     * @param unit The time unit for the timeout.
     *
     * @return The computed result.
     * @throws InterruptedException If the current thread was interrupted
     *         while waiting.
     * @throws TimeoutException If the wait timed out.
     */
    public final Result get(long timeout, /*TimeUnit*/Object unit){
    	return get();
    }
    /**
     * Executes the task with the specified parameters. The task returns
     * itself (this) so that the caller can keep a reference to it.
     *
     * This method must be invoked on the UI thread.
     *
     * @param params The parameters of the task.
     *
     * @return This instance of AsyncTask.
     *
     * @throws IllegalStateException If {@link #getStatus()} returns either
     *         {@link AsyncTask.Status#RUNNING} or {@link AsyncTask.Status#FINISHED}.
     */
    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = Status.RUNNING;
        mLocalParams = params;
        onPreExecute();
        
        /**
         * @j2sNative
         * var obj_this = this;
         * var _callback = function(){
         *     return {
         *         run:function() {
         *             mRunnable.bgRun(mRunnable.mLocalParams);
         *         }
         *     }
         * };
         * var _caller = _callback();  
         * mRunnable = obj_this;
         * mBgThread = Concurrent.Thread.create(_caller.run);
         * //console.log('bye');
         */{}
        

        return this;
    }
    protected final void bgRun(Params... params){
        mResult = doInBackground(params);
        System.out.println("doInBackround finished");
        Message message;
        message = sHandler.obtainMessage(MESSAGE_POST_RESULT,
                new AsyncTaskResult<Result>(AsyncTask.this, mResult));
        message.sendToTarget();
    }
    /**
     * This method can be invoked from {@link #doInBackground} to
     * publish updates on the UI thread while the background computation is
     * still running. Each call to this method will trigger the execution of
     * {@link #onProgressUpdate} on the UI thread.
     *
     * @param values The progress values to update the UI with.
     *
     * @see #onProgressUpdate
     * @see #doInBackground
     */
    protected final void publishProgress(Progress... values) {
    	System.out.println("Publish progress");
        sHandler.obtainMessage(MESSAGE_POST_PROGRESS,
                new AsyncTaskResult<Progress>(this, values)).sendToTarget();
    }

    protected void finish(Result result) {
        if (isCancelled()) result = null;
        onPostExecute(result);
        mStatus = Status.FINISHED;
    }
    
    
    
    private static class InternalHandler extends Handler{
    	public void handleMessage(Message msg) {
            AsyncTaskResult result = (AsyncTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                	if(result.mData==null){
                		result.mTask.finish(null);
                	}else{
                		result.mTask.finish(result.mData[0]);	
                	}
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
                case MESSAGE_POST_CANCEL:
                    result.mTask.onCancelled();
                    break;
            }
    	}
    }
    /**
     * Indicates the current status of the task. Each status will be set only once
     * during the lifetime of a task.
     */
    public enum Status {
        /**
         * Indicates that the task has not been executed yet.
         */
        PENDING,
        /**
         * Indicates that the task is running.
         */
        RUNNING,
        /**
         * Indicates that {@link AsyncTask#onPostExecute} has finished.
         */
        FINISHED,
    }

    @SuppressWarnings({"RawUseOfParameterizedType"})
    private static class AsyncTaskResult<Data> {
        final AsyncTask mTask;
        final Data[] mData;

        AsyncTaskResult(AsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }
}
