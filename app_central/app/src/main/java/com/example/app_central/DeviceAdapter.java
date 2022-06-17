package com.example.app_central;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder>  {

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mDeviceNameView;
        TextView mDeviceNameAddressView;
        TextView mDeviceSignal;
        ViewHolder(View view) {
            super(view);
            mDeviceNameView = view.findViewById(R.id.device_name);
            mDeviceNameAddressView = view.findViewById(R.id.device_address);
            mDeviceSignal = view.findViewById(R.id.signal_strength);
        }
    }

    private final ArrayList<ScanResult> mArrayList;
    private final Context mContext;


    public DeviceAdapter(Context context) {
        mArrayList = new ArrayList<>();
        mContext = context;
    }

    public DeviceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ScanResult scanResult = mArrayList.get(position);
        final String deviceName = scanResult.getDevice().getName();
        final String deviceAddress = scanResult.getDevice().getAddress();
         final String deviceSignal = Integer.toString(scanResult.getRssi());

        /*
        Temp scanResult = mArrayList.get(position);
        final String deviceName = scanResult.name;
        final String deviceAddress = scanResult.address;
        */

        if (TextUtils.isEmpty(deviceName)) {
            holder.mDeviceNameView.setText(mContext.getString(R.string.DeviceNameUnknown));
        } else {
            holder.mDeviceNameView.setText(deviceName);
        }

        if (TextUtils.isEmpty(deviceAddress)) {
            holder.mDeviceNameAddressView.setText(mContext.getString(R.string.DeviceAddressUnknown));
        } else {
            holder.mDeviceNameAddressView.setText(deviceAddress);
        }

        holder.mDeviceSignal.setText(String.format("%s dBm", deviceSignal));

    }

    @Override
    public int getItemCount() {
        return mArrayList.size();
    }

    public void add(ScanResult scanResult) {
        add(scanResult, true);
    }

    /**
     * Add a ScanResult item to the adapter if a result from that device isn't already present.
     * Otherwise updates the existing position with the new ScanResult.
     */
    public void add(ScanResult scanResult, boolean notify) {

        if (scanResult == null) {
            return;
        }

        int existingPosition = getPosition(scanResult.getDevice().getAddress());

        if (existingPosition >= 0) {
            // Device is already in list, update its record.
            mArrayList.set(existingPosition, scanResult);
        } else {
            // Add new Device's ScanResult to list.
            mArrayList.add(scanResult);
        }

        if (notify) {
            notifyDataSetChanged();
        }

    }

    public void add(List<ScanResult> scanResults) {
        if (scanResults != null) {
            for (ScanResult scanResult : scanResults) {
                add(scanResult, false);
            }
            notifyDataSetChanged();
        }
    }

    public void clearArrayList() {
        mArrayList.clear();
        notifyDataSetChanged();
    }

    /**
     * Search the adapter for an existing device address and return it, otherwise return -1.
     */
    private int getPosition(String address) {
        int position = -1;
        for (int i = 0; i < mArrayList.size(); i++) {
            if (mArrayList.get(i).getDevice().getAddress().equals(address)) {
                position = i;
                break;
            }
        }
        return position;
    }
}