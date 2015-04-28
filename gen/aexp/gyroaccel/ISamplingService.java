/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/paller/workspaces/android_dev/gyroaccel/src/aexp/gyroaccel/ISamplingService.aidl
 */
package aexp.gyroaccel;
public interface ISamplingService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements aexp.gyroaccel.ISamplingService
{
private static final java.lang.String DESCRIPTOR = "aexp.gyroaccel.ISamplingService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an aexp.gyroaccel.ISamplingService interface,
 * generating a proxy if needed.
 */
public static aexp.gyroaccel.ISamplingService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof aexp.gyroaccel.ISamplingService))) {
return ((aexp.gyroaccel.ISamplingService)iin);
}
return new aexp.gyroaccel.ISamplingService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_setCallback:
{
data.enforceInterface(DESCRIPTOR);
android.os.IBinder _arg0;
_arg0 = data.readStrongBinder();
this.setCallback(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_removeCallback:
{
data.enforceInterface(DESCRIPTOR);
this.removeCallback();
reply.writeNoException();
return true;
}
case TRANSACTION_stopSampling:
{
data.enforceInterface(DESCRIPTOR);
this.stopSampling();
reply.writeNoException();
return true;
}
case TRANSACTION_isSampling:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isSampling();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements aexp.gyroaccel.ISamplingService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void setCallback(android.os.IBinder binder) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder(binder);
mRemote.transact(Stub.TRANSACTION_setCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void removeCallback() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_removeCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void stopSampling() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stopSampling, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public boolean isSampling() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isSampling, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int getState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_setCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_removeCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_stopSampling = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_isSampling = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public void setCallback(android.os.IBinder binder) throws android.os.RemoteException;
public void removeCallback() throws android.os.RemoteException;
public void stopSampling() throws android.os.RemoteException;
public boolean isSampling() throws android.os.RemoteException;
public int getState() throws android.os.RemoteException;
}
