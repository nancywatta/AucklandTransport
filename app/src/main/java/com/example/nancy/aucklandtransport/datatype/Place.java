package com.example.nancy.aucklandtransport.datatype;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Place class represents a nearby location with the information like latitude,
 * longitude, place name, vicinity and photos. We are making this as a Parcelable
 * class in order to retain the instances of this class during screen rotation.
 *
 * Created by Nancy on 9/9/14.
 */
public class Place implements Parcelable {
    // Latitude of the place
    public String mLat="";

    // Longitude of the place
    public String mLng="";

    // Place Name
    public String mPlaceName="";

    // Vicinity of the place
    public String mVicinity="";

    // Photos of the place
    // Photo is a Parcelable class
    public Photo[] mPhotos={};

    // If the place has been added in the list to be visited
    // when using Tourist Planner functionality.
    public Boolean isAdded = false;

    // Stores the Duration for how long a user would like
    // to stay at this Place.
    public int duration;

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    /** Writing Place object data to Parcel */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mLat);
        dest.writeString(mLng);
        dest.writeString(mPlaceName);
        dest.writeString(mVicinity);
        dest.writeInt(duration);
        dest.writeByte((byte) (isAdded ? 1 : 0));
        dest.writeParcelableArray(mPhotos, 0);
    }

    public Place(){
    }

    /** Initializing Place object from Parcel object */
    private Place(Parcel in){
        this.mLat = in.readString();
        this.mLng = in.readString();
        this.mPlaceName = in.readString();
        this.mVicinity = in.readString();
        this.isAdded = in.readByte() != 0;
        this.duration = in.readInt();
        this.mPhotos = (Photo[])in.readParcelableArray(Photo.class.getClassLoader());
    }

    /** Generates an instance of Place class from Parcel */
    public static final Parcelable.Creator<Place> CREATOR = new Parcelable.Creator<Place>(){
        @Override
        public Place createFromParcel(Parcel source) {
            return new Place(source);
        }

        @Override
        public Place[] newArray(int size) {
            // TODO Auto-generated method stub
            return null;
        }
    };

    public boolean compare(Place place) {
        if(this.mLat.compareTo(place.mLat) ==0 && this.mLng.compareTo(place.mLng)==0
                && this.mPlaceName.compareTo(place.mPlaceName) ==0 )
            return true;
        return false;
    }

}
