path(path, '$straight_path');


prm.spectralUpdateInterval = $frameshift;
prm.F0frameUpdateInterval = $frameshift;
prm.F0searchLowerBound = $mini_f0;
prm.F0searchUpperBound = $maxi_f0;


[x, fs] = audioread('$input_file_name');
[f0, ap] = exstraightsource(x, fs, prm);
[sp] = exstraightspec(x, f0, fs, prm);


ap = ap';
sp = sp';
sp = sp * $norm_coef;


f_f0 = fopen('$f0_output', 'wb')
fwrite(f_f0, f0, 'float32')
fclose(f_f0);

f_sp = fopen('$sp_output', 'wb')
fwrite(f_sp, sp, 'float32')
fclose(f_sp);

f_ap = fopen('$ap_output', 'wb')
fwrite(f_ap, ap, 'float32')
fclose(f_ap);
