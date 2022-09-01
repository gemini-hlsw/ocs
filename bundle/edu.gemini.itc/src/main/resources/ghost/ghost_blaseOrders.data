function BF = BlazeFunction(lambda)
%
%   Blaze function for the individual echelle orders. Uses approximations which
%   assume orders m are all high. Based on Schroeder 'Astronomical Optics' p335;
%   GHOST 3 124. It uses data on order centres vs order number from annotations on
%   echellograms from PDD - may be out of date? 
%   ** It also assumes exactly Littrow operation of the echelle, since effective
%   groove width, and hence single aperture diffraction pattern, are different if not
%   exactly Littrow ... **
%
%   Input parameters:
%   ----------------
%   lambda   : wavelength at which blaze function is required. Single scalar or 
%                    vector of values, nm.
%
%   Output parameters:
%   ------------------
%   BF       : blaze function value at each lambda. It is 1 at blaze wavelength 
%                    i.e. centre, and drops to 0.405 at FSR edge.
%
%                                                       G. Robertson 25 June 2019
%
%   Get echelle order data
%
    load('RefData','Echelle_orders')
    m_vec = Echelle_orders(:,1);
    lambda_B_vec = Echelle_orders(:,2);
    [m_len,~] = size(Echelle_orders);
    lambda = lambda(:).';  % ensures row vector 
    [dim1,~] = size(lambda);
    assert(dim1 == 1,'lambda is not a scalar or vector!')
%
%   To find which lambda_B is closest to each element of lambda, start by making
%   matrix of the differences. Then use min to find closest.
%
    for i = 1:m_len
        Diff_matrix(i,:) = abs(lambda - lambda_B_vec(i));
    end   
    [~,I] = min(Diff_matrix);  % I is row vector with same length as lambda, giving 
%                                element in Echelle_orders which has closest value
%
    lambda_B_use = lambda_B_vec(I);
    m_use = m_vec(I);
    lambda_B_use = lambda_B_use(:).';     % Change them back to row vectors
    m_use = m_use(:).';     
% 
    nu = pi*m_use.*(lambda - lambda_B_use)./lambda_B_use;                          
    BF = (sin(nu)./(nu+5*eps)).^2 + (nu == 0);    % This puts 1 in answer when nu = 0
    return
%
end

