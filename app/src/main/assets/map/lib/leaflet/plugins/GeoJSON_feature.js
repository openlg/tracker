(function() {
		L.GeoJSON.Encoded = L.GeoJSON.extend({
			initialize: function(b, a) {
				L.GeoJSON.prototype.initialize.call(this, b, a)
			},
			defaultOptions: function(a) {
				if (typeof a === "number") {
					a = {
						precision: a
					}
				} else {
					a = a || {}
				}
				a.precision = a.precision || 5;
				a.factor = a.factor || Math.pow(10, a.precision);
				a.dimension = a.dimension || 2;
				return a
			},
			_decodeFeature: function(c) {
				var d = this, b, e, g;
				function h(k) {
					var l = [];
					for (var i = 0; i < k.length; i++) {
						l.push(d.decode(k[i]))
					}
					return l
				}
				function f(l) {
					var j = [];
					for (var k = 0; k < l.length; k++) {
						j.push(h(l[k]))
					}
					return j
				}
				b = c.geometry.coordinates;
				switch (c.geometry.type) {
					case "Point":
						g = L.marker([b[1], b[0]]);
						break;
					case "LineString":
						e = L.Util.isArray(b[0]) ? L.GeoJSON.coordsToLatLngs(b, 0) : h(b[0])[0];
						g = L.polyline(e);
						break;
					case "MultiLineString":
						e = L.Util.isArray(b[0][0]) ? L.GeoJSON.coordsToLatLngs(b, 1) : h(b);
						//g = L.multiPolyline(e);
						g = L.polyline(e);
						break;
					case "Polygon":
						e = L.Util.isArray(b[0][0]) ? L.GeoJSON.coordsToLatLngs(b, 1) : h(b);
						g = L.polygon(e);
						break;
					case "MultiPolygon":
						e = L.Util.isArray(b[0][0]) ? L.GeoJSON.coordsToLatLngs(b, 2) : f(b);
						//g = L.multiPolygon(e);
						g = L.polygon(e);
						break
				}
				var a = g.toGeoJSON();
				if (a.properties) {
					a.properties = c.properties
				}
				return a
			},
			addData: function(b) {
				var g = L.Util.isArray(b) ? b : b.features, f, a, e;
				if (g) {
					for (f = 0,
						     a = g.length; f < a; f++) {
						e = this._decodeFeature(g[f]);
						if (e.geometries || e.geometry || e.features || e.coordinates) {
							this.addData(e)
						}
					}
					return this
				}
				var c = this.options;
				if (c.filter && !c.filter(b)) {
					return this
				}
				var d = L.GeoJSON.geometryToLayer(b, c);
				d.feature = L.GeoJSON.asFeature(b);
				d.defaultOptions = d.options;
				this.resetStyle(d);
				if (c.onEachFeature) {
					c.onEachFeature(b, d)
				}
				return this.addLayer(d)
			}
		});
		L.GeoJSON.Encoded.include({
			decode: function(h, c) {
				c = this.defaultOptions(c);
				var f = this.decodeDeltas(h, c)
					, e = [];
				for (var d = 0, b = f.length; d + (c.dimension - 1) < b; ) {
					var a = [];
					for (var g = 0; g < c.dimension; ++g) {
						a.push(f[d++])
					}
					e.push(a)
				}
				return e
			},
			decodeDeltas: function(g, c) {
				c = this.defaultOptions(c);
				var b = this.decodeFloats(g, c)
					, f = [];
				for (var e = 0, a = b.length; e < a; ) {
					for (var h = 0; h < c.dimension; ++h,
						++e) {
						b[e] = Math.round((f[h] = b[e] + (f[h] || 0)) * c.factor) / c.factor
					}
				}
				return b
			},
			decodeFloats: function(e, c) {
				c = this.defaultOptions(c);
				var b = this.decodeSignedIntegers(e);
				for (var d = 0, a = b.length; d < a; ++d) {
					b[d] /= c.factor
				}
				return b
			},
			decodeSignedIntegers: function(d) {
				var b = this.decodeUnsignedIntegers(d);
				for (var c = 0, a = b.length; c < a; ++c) {
					b[c] = (b[c] & 1) ? ~(b[c] >> 1) : (b[c] >> 1)
				}
				return b
			},
			decodeUnsignedIntegers: function(h) {
				var e = []
					, g = 0
					, d = 0;
				for (var f = 0, c = h.length; f < c; ++f) {
					var a = h.charCodeAt(f) - 63;
					g |= (a & 31) << d;
					if (a < 32) {
						e.push(g);
						g = 0;
						d = 0
					} else {
						d += 5
					}
				}
				return e
			}
		});
		L.geoJson.encoded = function(b, a) {
			return new L.GeoJSON.Encoded(b,a)
		}
	}
).call(this);
