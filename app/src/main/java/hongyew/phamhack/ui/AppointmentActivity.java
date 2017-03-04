package hongyew.phamhack.ui;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.ArrayList;
import java.util.List;

import hongyew.phamhack.AppPreference_;
import hongyew.phamhack.MainApplication;
import hongyew.phamhack.MainApplication_;
import hongyew.phamhack.R;
import hongyew.phamhack.manager.AppointmentManager;
import hongyew.phamhack.manager.AppointmentManager_;
import hongyew.phamhack.model.Appointment;

import static android.view.View.GONE;

@EActivity(R.layout.appointment_activity)
public class AppointmentActivity extends AppCompatActivity {
    @App
    MainApplication application;
    
    @Bean
    AppointmentManager appointmentManager;
    
    @Pref
    AppPreference_ pref;
    
    @ViewById(R.id.appointment_list)
    RecyclerView appointmentView;
    
    AppointmentAdapter adapter;
    
    ChildEventListener appointmentEvent;

    @AfterViews
    protected void init() {
        adapter = new AppointmentAdapter(this);
        appointmentView.setLayoutManager(new LinearLayoutManager(this));
        appointmentView.setAdapter(adapter);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        appointmentEvent = new ChildEventListener() {
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                onChildChanged(dataSnapshot, s);
            }
        
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Appointment a = dataSnapshot.getValue(Appointment.class);
                int index = a.time.intValue() - 9;
                if (index >= 0 && index < adapter.getItemCount()) {
                    Appointment appointment = adapter.getItem(index);
                    appointment.busy = true;
                    appointment.user = a.user;
                    appointment.key = dataSnapshot.getKey();
                    adapter.notifyDataSetChanged();
                }
            }
        
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Appointment a = dataSnapshot.getValue(Appointment.class);
                a.key = dataSnapshot.getKey();
                int index = a.time.intValue() - 9;
                if (index >= 0 && index < adapter.getItemCount()) {
                    adapter.getItem(index).busy = false;
                    adapter.notifyDataSetChanged();
                }
            }
        
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                onChildChanged(dataSnapshot, s);
            }
        
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        appointmentManager.appointmentRef().addChildEventListener(appointmentEvent);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        appointmentManager.appointmentRef().removeEventListener(appointmentEvent);
    }
    
    static class AppointmentAdapter extends RecyclerView.Adapter<AppointmentViewHolder> {
        List<Appointment> appointments = new ArrayList<>();
        Context context;
        
        public AppointmentAdapter(Context context) {
            this.context = context;
            for (int i=9;i<=16;i++) {
                Appointment a = new Appointment();
                a.time = (long) i;
                appointments.add(a);
            }
        }
    
        public Context getContext() {
            return context;
        }
    
        @Override
        public AppointmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.appointment_item, null);
            return new AppointmentViewHolder(view);
        }
    
        @Override
        public void onBindViewHolder(AppointmentViewHolder holder, int position) {
            Appointment appointment = appointments.get(position);
            holder.hour24 = appointment.time;
            holder.hour12 = appointment.time;
            if (appointment.time > 12) {
                holder.hour12 = appointment.time - 12;
            }
            String meridiem = (appointment.time >= 12)?"pm":"am";
            holder.hourView.setText(holder.hour12 + " " + meridiem);
            holder.appointmentKey = appointment.key;
            holder.enable(true);
            holder.setCheckBox(false);
            holder.adapter = this;
            if (appointment.busy) {
                if (isYourAppointment(appointment)) {
                    holder.setCheckBox(true);
                    holder.enable(true, "Booked", R.color.md_green_700);
                }
                else {
                    holder.enable(false, "Unavailable", R.color.md_red_700);
                }
            }
            else {
                holder.enable(!appointment.disabled);
            }
        }
    
        boolean isYourAppointment(Appointment appointment) {
            if (appointment.user != null && appointment.user.name != null) {
                return appointment.user.name.equals(MainApplication_.getInstance().appointmentSelf().name);
            }
            return false;
        }
        
        @Override
        public int getItemCount() {
            return appointments.size();
        }
        
        public Appointment getItem(int i) {
            return appointments.get(i);
        }
    }
    
    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        public Context context;
        public TextView hourView;
        public TextView availabilityView;
        public AppCompatCheckBox checkView;
        public RelativeLayout layout;
        public long hour12;
        public long hour24;
        public String appointmentKey;
        public CompoundButton.OnCheckedChangeListener checkListener;
        public AppointmentAdapter adapter;
        
        public AppointmentViewHolder(final View itemView) {
            super(itemView);
            this.context = itemView.getContext();
            layout = (RelativeLayout) itemView.findViewById(R.id.appointment_item_layout);
            hourView = (TextView) itemView.findViewById(R.id.appointment_hour);
            availabilityView = (TextView) itemView.findViewById(R.id.appointment_availability);
            checkView = (AppCompatCheckBox) itemView.findViewById(R.id.appointment_checkbox);
    
            checkListener = new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    MainApplication application = MainApplication_.getInstance();
                    AppPreference_ pref = new AppPreference_(application);
                    AppointmentManager appointmentManager = AppointmentManager_.getInstance_(itemView.getContext());
            
                    // Add
                    if (checked) {
                        Appointment a = new Appointment();
                        a.time = hour24;
                        a.user = application.appointmentSelf();
                
                        DatabaseReference ref = appointmentManager.appointmentRef().push();
                        a.key = ref.getKey();
                        ref.setValue(a);
                        pref.appointmentKey().put(a.key);
                        lockAllExceptKey(true, a.key);
                    }
                    // Remove
                    else {
                        DatabaseReference ref = appointmentManager.appointmentRef().child(appointmentKey);
                        ref.removeValue();
                        lockAllExceptKey(false, null);
                    }
                }
            };
            checkView.setOnCheckedChangeListener(checkListener);
        }
        
        void lockAllExceptKey(boolean b, String key) {
            for (int i=0; i<adapter.getItemCount();i++) {
                Appointment a = adapter.getItem(i);
                if (key == null || !key.equals(a.key)) {
                    a.disabled = b;
                }
            }
        }
    
        public void enable(boolean b) {
            enable(b, null, 0);
        }
        
        public void enable(boolean b, String status, int color) {
            hourView.setEnabled(b);
            checkView.setEnabled(b);
            if (b) {
                layout.setBackgroundColor(context.getResources().getColor(R.color.white));
            }
            else {
                layout.setBackgroundColor(context.getResources().getColor(R.color.disabled_appointment));
            }
            if (status != null) {
                availabilityView.setText(status);
                availabilityView.setTextColor(context.getResources().getColor(color));
                availabilityView.setVisibility(View.VISIBLE);
            }
            else {
                availabilityView.setVisibility(GONE);
            }
        }
        
        public void setCheckBox(boolean b) {
            enableCheckListener(false);
            checkView.setChecked(b);
            enableCheckListener(true);
        }
        
        public void enableCheckListener(boolean b) {
            if (b) {
                checkView.setOnCheckedChangeListener(checkListener);
            }
            else {
                checkView.setOnCheckedChangeListener(null);
            }
        }
    }
}
