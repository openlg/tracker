(function(){

var map = L.map('map', {
	center: [23.926013, 109.473267],
	zoom: 8,
	doubleClickZoom: false
});

L.tileLayer.chinaProvider('TianDiTu.Normal.Map', {}).addTo(map);
L.tileLayer.chinaProvider('TianDiTu.Normal.Annotion', {}).addTo(map);
//L.tileLayer.chinaProvider('GaoDe.Normal.Map', {maxZoom:18,minZoom:1, attribution: 'Map data &copy; 高德软件'}).addTo(map);
//L.tileLayer.chinaProvider('Google.Normal.Map', {}).addTo(map);
//L.tileLayer.chinaProvider('Geoq.Normal.Cold', {}).addTo(map);

var fns = {
    setCurrentLocation : function(lon, lat){

        L.circle(L.latLng(lat, lon), 5).addTo(map)

    }
};

window.loadData = function(data){
    if( data ){
        if( data.type === 'currentLocation')
            fns.setCurrentLocation(data.lon, data.lat);
    }
};

})();