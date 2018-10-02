path(path, '$straight_path');

% Define parameters
prm.spectralUpdateInterval = $frameshift;
prm.F0frameUpdateInterval = $frameshift;
prm.F0searchLowerBound = $mini_f0;
prm.F0searchUpperBound = $maxi_f0;

% Extract coefficients
[x, fs] = audioread('$input_file_name');
[f0, ap] = exstraightsource(x, fs, prm);
[sp] = exstraightspec(x, f0, fs, prm);
sp = sp * $norm_coef;

% Save the coefficients
f_f0 = fopen('$f0_output', 'wb');
fwrite(f_f0, f0, 'float');
fclose(f_f0);

f_sp = fopen('$sp_output', 'wb');
fwrite(f_sp, sp, 'float');
fclose(f_sp);

f_ap = fopen('$ap_output', 'wb');
fwrite(f_ap, ap, 'float');
fclose(f_ap);
